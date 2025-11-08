using System.Text.Json;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Domain.Exceptions;
using ProjectService.Dtos;
using ProjectService.Dtos.ChangeRequests;

namespace ProjectService.Services;

public class ProjectWorkflowService : IProjectWorkflowService
{
    private static readonly IReadOnlyDictionary<ProjectStatus, ProjectStatus[]> AllowedTransitions =
        new Dictionary<ProjectStatus, ProjectStatus[]>
        {
            [ProjectStatus.Requested] = new[] { ProjectStatus.Quoted, ProjectStatus.Cancelled },
            [ProjectStatus.Quoted] = new[] { ProjectStatus.Approved, ProjectStatus.Cancelled },
            [ProjectStatus.Approved] = new[] { ProjectStatus.InProgress, ProjectStatus.Cancelled },
            [ProjectStatus.InProgress] = new[] { ProjectStatus.Completed, ProjectStatus.Cancelled },
            [ProjectStatus.Completed] = Array.Empty<ProjectStatus>(),
            [ProjectStatus.Cancelled] = Array.Empty<ProjectStatus>()
        };

    private static readonly IReadOnlyDictionary<ProjectStatus, int> ProjectStatusOrder =
        new Dictionary<ProjectStatus, int>
        {
            [ProjectStatus.Requested] = 0,
            [ProjectStatus.Quoted] = 1,
            [ProjectStatus.Approved] = 2,
            [ProjectStatus.InProgress] = 3,
            [ProjectStatus.Completed] = 4,
            [ProjectStatus.Cancelled] = 5
        };

    private readonly AppDb _db;
    private readonly ILogger<ProjectWorkflowService> _logger;
    private readonly JsonSerializerOptions _serializerOptions = new(JsonSerializerDefaults.Web);

    public ProjectWorkflowService(AppDb db, ILogger<ProjectWorkflowService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task<Project> CreateProjectAsync(CreateProjectRequest request, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.Projects.AsNoTracking()
                .FirstOrDefaultAsync(p => p.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                return existing;
            }
        }

        if (request.CustomerId == Guid.Empty)
        {
            throw new DomainException("CustomerId is required.");
        }

        var title = request.Title?.Trim();
        if (string.IsNullOrWhiteSpace(title))
        {
            throw new DomainException("Title is required.");
        }

        var now = DateTimeOffset.UtcNow;
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = request.CustomerId,
            Title = title,
            Status = ProjectStatus.Requested,
            CreatedAt = now,
            UpdatedAt = now,
            Budget = 0m,
            ClientRequestId = clientRequestId
        };

        await _db.Projects.AddAsync(project, cancellationToken);
        await _db.StatusHistory.AddAsync(new StatusHistory
        {
            ProjectId = project.ProjectId,
            FromStatus = ProjectStatus.Requested,
            ToStatus = ProjectStatus.Requested,
            ChangedBy = actorId == Guid.Empty ? project.CustomerId : actorId,
            ChangedAt = now,
            Note = "Project created"
        }, cancellationToken);

        EnqueueEvent(
            topic: "project.created",
            projectId: project.ProjectId,
            quoteId: null,
            changeRequestId: null,
            fromStatus: ProjectStatus.Requested.ToString(),
            toStatus: ProjectStatus.Requested.ToString(),
            actor: new ActorContext(actorId == Guid.Empty ? project.CustomerId : actorId, actorRole),
            occurredAt: now,
            metadata: new { project.CustomerId, project.Title });

        await _db.SaveChangesAsync(cancellationToken);
        return project;
    }

    public async Task<Project?> GetProjectAsync(Guid projectId, CancellationToken cancellationToken)
    {
        return await _db.Projects
            .Include(x => x.Tasks)
            .Include(x => x.Quotes)
            .Include(x => x.StatusHistory)
            .Include(x => x.ChangeRequests)
            .AsNoTracking()
            .FirstOrDefaultAsync(x => x.ProjectId == projectId, cancellationToken);
    }

    public async Task<IReadOnlyList<Project>> GetProjectsAsync(ProjectStatus? status, Guid? customerId, CancellationToken cancellationToken)
    {
        var query = _db.Projects
            .Include(x => x.Tasks)
            .Include(x => x.Quotes)
            .Include(x => x.StatusHistory)
            .AsNoTracking()
            .AsQueryable();

        if (status.HasValue)
        {
            query = query.Where(x => x.Status == status.Value);
        }

        if (customerId.HasValue)
        {
            query = query.Where(x => x.CustomerId == customerId.Value);
        }

        return await query
            .OrderByDescending(x => x.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<StatusHistory>> GetStatusHistoryAsync(Guid projectId, CancellationToken cancellationToken)
    {
        return await _db.StatusHistory
            .Where(h => h.ProjectId == projectId)
            .OrderByDescending(h => h.ChangedAt)
            .AsNoTracking()
            .ToListAsync(cancellationToken);
    }

    public async Task<Project> UpdateStatusAsync(Guid projectId, ProjectStatus newStatus, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        var project = await _db.Projects.FirstOrDefaultAsync(x => x.ProjectId == projectId, cancellationToken);
        if (project is null)
        {
            throw new DomainException("Project not found.", StatusCodes.Status404NotFound);
        }

        if (project.Status == newStatus)
        {
            return project;
        }

        if (!AllowedTransitions.TryGetValue(project.Status, out var allowed) || !allowed.Contains(newStatus))
        {
            throw new DomainException($"Cannot transition project from {project.Status} to {newStatus}.", StatusCodes.Status409Conflict);
        }

        var now = DateTimeOffset.UtcNow;
        var previous = project.Status;
        project.Status = newStatus;
        project.UpdatedAt = now;

        await _db.StatusHistory.AddAsync(new StatusHistory
        {
            ProjectId = project.ProjectId,
            FromStatus = previous,
            ToStatus = newStatus,
            ChangedBy = actorId,
            ChangedAt = now,
            Note = $"Status updated to {newStatus}"
        }, cancellationToken);

        EnqueueEvent(
            topic: "project.updated",
            projectId: project.ProjectId,
            quoteId: null,
            changeRequestId: null,
            fromStatus: previous.ToString(),
            toStatus: newStatus.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { project.CustomerId, project.Title });

        await _db.SaveChangesAsync(cancellationToken);
        return project;
    }

    public async Task<Quote> CreateQuoteAsync(Guid projectId, CreateQuoteRequest request, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.Quotes.AsNoTracking()
                .FirstOrDefaultAsync(q => q.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                if (existing.ProjectId != projectId)
                {
                    throw new DomainException("Idempotency key already used for another project.", StatusCodes.Status409Conflict);
                }

                return existing;
            }
        }

        var project = await _db.Projects.FirstOrDefaultAsync(p => p.ProjectId == projectId, cancellationToken);
        if (project is null)
        {
            throw new DomainException("Project not found.", StatusCodes.Status404NotFound);
        }

        if (request.Total <= 0)
        {
            throw new DomainException("Quote total must be greater than zero.");
        }

        var now = DateTimeOffset.UtcNow;
        var quote = new Quote
        {
            QuoteId = Guid.NewGuid(),
            ProjectId = projectId,
            Total = request.Total,
            Status = QuoteStatus.Draft,
            IssuedAt = now,
            ClientRequestId = clientRequestId
        };

        await _db.Quotes.AddAsync(quote, cancellationToken);
        project.UpdatedAt = now;
        await _db.SaveChangesAsync(cancellationToken);
        return quote;
    }

    public async Task<Quote> ApproveQuoteAsync(Guid quoteId, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.Quotes.AsNoTracking()
                .FirstOrDefaultAsync(q => q.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                if (existing.QuoteId != quoteId)
                {
                    throw new DomainException("Idempotency key already used for another quote.", StatusCodes.Status409Conflict);
                }

                return existing;
            }
        }

        var quote = await _db.Quotes
            .Include(x => x.Project)
            .FirstOrDefaultAsync(x => x.QuoteId == quoteId, cancellationToken);

        if (quote is null)
        {
            throw new DomainException("Quote not found.", StatusCodes.Status404NotFound);
        }

        if (quote.Status == QuoteStatus.Approved)
        {
            return quote;
        }

        if (quote.Status != QuoteStatus.Draft)
        {
            throw new DomainException($"Quote in status {quote.Status} cannot be approved.", StatusCodes.Status409Conflict);
        }

        var now = DateTimeOffset.UtcNow;
        quote.Status = QuoteStatus.Approved;
        quote.ApprovedAt = now;
        quote.ApprovedBy = actorId;
        quote.ClientRequestId = clientRequestId;

        EnqueueEvent(
            topic: "quote.approved",
            projectId: quote.ProjectId,
            quoteId: quote.QuoteId,
            changeRequestId: null,
            fromStatus: QuoteStatus.Draft.ToString(),
            toStatus: QuoteStatus.Approved.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { quote.Total });

        if (quote.Project is not null)
        {
            var previous = quote.Project.Status;
            quote.Project.Budget = quote.Total;
            quote.Project.UpdatedAt = now;

            if (IsEarlierThan(previous, ProjectStatus.Approved))
            {
                quote.Project.Status = ProjectStatus.Approved;

                await _db.StatusHistory.AddAsync(new StatusHistory
                {
                    ProjectId = quote.Project.ProjectId,
                    FromStatus = previous,
                    ToStatus = ProjectStatus.Approved,
                    ChangedBy = actorId,
                    ChangedAt = now,
                    Note = "Quote approved"
                }, cancellationToken);

                EnqueueEvent(
                    topic: "project.updated",
                    projectId: quote.Project.ProjectId,
                    quoteId: quote.QuoteId,
                    changeRequestId: null,
                    fromStatus: previous.ToString(),
                    toStatus: ProjectStatus.Approved.ToString(),
                    actor: new ActorContext(actorId, actorRole),
                    occurredAt: now,
                    metadata: new { quote.Project.CustomerId, quote.Project.Title, quote.Project.Budget });
            }
        }

        await _db.SaveChangesAsync(cancellationToken);
        return quote;
    }

    public async Task<Quote> RejectQuoteAsync(Guid quoteId, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.Quotes.AsNoTracking()
                .FirstOrDefaultAsync(q => q.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                if (existing.QuoteId != quoteId)
                {
                    throw new DomainException("Idempotency key already used for another quote.", StatusCodes.Status409Conflict);
                }

                return existing;
            }
        }

        var quote = await _db.Quotes
            .Include(x => x.Project)
            .FirstOrDefaultAsync(x => x.QuoteId == quoteId, cancellationToken);

        if (quote is null)
        {
            throw new DomainException("Quote not found.", StatusCodes.Status404NotFound);
        }

        if (quote.Status == QuoteStatus.Rejected)
        {
            return quote;
        }

        if (quote.Status != QuoteStatus.Draft)
        {
            throw new DomainException($"Quote in status {quote.Status} cannot be rejected.", StatusCodes.Status409Conflict);
        }

        var now = DateTimeOffset.UtcNow;
        quote.Status = QuoteStatus.Rejected;
        quote.RejectedAt = now;
        quote.RejectedBy = actorId;
        quote.ClientRequestId = clientRequestId;

        if (quote.Project is not null)
        {
            quote.Project.UpdatedAt = now;
        }

        EnqueueEvent(
            topic: "quote.rejected",
            projectId: quote.ProjectId,
            quoteId: quote.QuoteId,
            changeRequestId: null,
            fromStatus: QuoteStatus.Draft.ToString(),
            toStatus: QuoteStatus.Rejected.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { quote.Total });

        await _db.SaveChangesAsync(cancellationToken);
        return quote;
    }

    public async Task<ChangeRequest> CreateChangeRequestAsync(Guid projectId, CreateChangeRequestRequest request, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.ChangeRequests.AsNoTracking()
                .FirstOrDefaultAsync(cr => cr.ProjectId == projectId && cr.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                return existing;
            }
        }

        var project = await _db.Projects.FirstOrDefaultAsync(p => p.ProjectId == projectId, cancellationToken);
        if (project is null)
        {
            throw new DomainException("Project not found.", StatusCodes.Status404NotFound);
        }

        var title = request.Title?.Trim();
        if (string.IsNullOrWhiteSpace(title))
        {
            throw new DomainException("Title is required.");
        }

        var now = DateTimeOffset.UtcNow;
        var changeRequest = new ChangeRequest
        {
            ChangeRequestId = Guid.NewGuid(),
            ProjectId = projectId,
            Title = title,
            Description = request.Description,
            ProposedPriceDelta = request.ProposedPriceDelta,
            ProposedExtraHours = request.ProposedExtraHours,
            ProposedNewDueDate = request.ProposedNewDueDate,
            Status = ChangeRequestStatus.Submitted,
            CreatedBy = actorId,
            CreatedAt = now,
            ClientRequestId = clientRequestId
        };

        await _db.ChangeRequests.AddAsync(changeRequest, cancellationToken);
        project.UpdatedAt = now;

        await _db.StatusHistory.AddAsync(new StatusHistory
        {
            ProjectId = project.ProjectId,
            FromStatus = project.Status,
            ToStatus = project.Status,
            ChangedBy = actorId,
            ChangedAt = now,
            Note = $"CR submitted: {title}"
        }, cancellationToken);

        EnqueueEvent(
            topic: "project.change-request.created",
            projectId: project.ProjectId,
            quoteId: null,
            changeRequestId: changeRequest.ChangeRequestId,
            fromStatus: changeRequest.Status.ToString(),
            toStatus: changeRequest.Status.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { changeRequest.Title },
            deltas: new
            {
                changeRequest.ProposedPriceDelta,
                changeRequest.ProposedExtraHours,
                changeRequest.ProposedNewDueDate
            });

        await _db.SaveChangesAsync(cancellationToken);
        return changeRequest;
    }

    public async Task<IReadOnlyList<ChangeRequest>> GetProjectChangeRequestsAsync(Guid projectId, CancellationToken cancellationToken)
    {
        return await _db.ChangeRequests
            .Where(cr => cr.ProjectId == projectId)
            .OrderByDescending(cr => cr.CreatedAt)
            .AsNoTracking()
            .ToListAsync(cancellationToken);
    }

    public async Task<ChangeRequest?> GetChangeRequestAsync(Guid changeRequestId, CancellationToken cancellationToken)
    {
        return await _db.ChangeRequests
            .AsNoTracking()
            .FirstOrDefaultAsync(cr => cr.ChangeRequestId == changeRequestId, cancellationToken);
    }

    public async Task<ChangeRequest> ApproveChangeRequestAsync(Guid changeRequestId, uint rowVersion, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        var changeRequest = await _db.ChangeRequests
            .Include(cr => cr.Project)
            .FirstOrDefaultAsync(cr => cr.ChangeRequestId == changeRequestId, cancellationToken);

        if (changeRequest is null)
        {
            throw new DomainException("Change request not found.", StatusCodes.Status404NotFound);
        }

        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.ChangeRequests.AsNoTracking()
                .FirstOrDefaultAsync(cr => cr.ProjectId == changeRequest.ProjectId && cr.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                if (existing.ChangeRequestId != changeRequestId)
                {
                    throw new DomainException("Idempotency key already used for another change request.", StatusCodes.Status409Conflict);
                }

                return existing;
            }
        }

        EnsureChangeRequestRowVersion(changeRequest, rowVersion);

        var previousStatus = changeRequest.Status;
        if (previousStatus == ChangeRequestStatus.Approved)
        {
            return changeRequest;
        }

        if (previousStatus is ChangeRequestStatus.Rejected or ChangeRequestStatus.Applied)
        {
            throw new DomainException($"Cannot approve change request in status {changeRequest.Status}.", StatusCodes.Status409Conflict);
        }

        var now = DateTimeOffset.UtcNow;
        changeRequest.Status = ChangeRequestStatus.Approved;
        changeRequest.DecidedBy = actorId;
        changeRequest.DecidedAt = now;
        changeRequest.ClientRequestId = clientRequestId;

        if (changeRequest.Project is not null)
        {
            var previous = changeRequest.Project.Status;
            changeRequest.Project.UpdatedAt = now;

            if (IsEarlierThan(previous, ProjectStatus.Approved))
            {
                changeRequest.Project.Status = ProjectStatus.Approved;

                await _db.StatusHistory.AddAsync(new StatusHistory
                {
                    ProjectId = changeRequest.Project.ProjectId,
                    FromStatus = previous,
                    ToStatus = ProjectStatus.Approved,
                    ChangedBy = actorId,
                    ChangedAt = now,
                    Note = $"CR approved: {changeRequest.Title}"
                }, cancellationToken);

                EnqueueEvent(
                    topic: "project.updated",
                    projectId: changeRequest.Project.ProjectId,
                    quoteId: null,
                    changeRequestId: changeRequest.ChangeRequestId,
                    fromStatus: previous.ToString(),
                    toStatus: ProjectStatus.Approved.ToString(),
                    actor: new ActorContext(actorId, actorRole),
                    occurredAt: now,
                    metadata: new { changeRequest.Project.CustomerId, changeRequest.Project.Title });
            }
            else
            {
                await _db.StatusHistory.AddAsync(new StatusHistory
                {
                    ProjectId = changeRequest.Project.ProjectId,
                    FromStatus = previous,
                    ToStatus = previous,
                    ChangedBy = actorId,
                    ChangedAt = now,
                    Note = $"CR approved: {changeRequest.Title}"
                }, cancellationToken);
            }
        }

        EnqueueEvent(
            topic: "project.change-request.approved",
            projectId: changeRequest.ProjectId,
            quoteId: null,
            changeRequestId: changeRequest.ChangeRequestId,
            fromStatus: previousStatus.ToString(),
            toStatus: ChangeRequestStatus.Approved.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { changeRequest.Title },
            deltas: new
            {
                changeRequest.ProposedPriceDelta,
                changeRequest.ProposedExtraHours,
                changeRequest.ProposedNewDueDate
            });

        await _db.SaveChangesAsync(cancellationToken);
        return changeRequest;
    }

    public async Task<ChangeRequest> RejectChangeRequestAsync(Guid changeRequestId, uint rowVersion, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        var changeRequest = await _db.ChangeRequests
            .Include(cr => cr.Project)
            .FirstOrDefaultAsync(cr => cr.ChangeRequestId == changeRequestId, cancellationToken);

        if (changeRequest is null)
        {
            throw new DomainException("Change request not found.", StatusCodes.Status404NotFound);
        }

        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.ChangeRequests.AsNoTracking()
                .FirstOrDefaultAsync(cr => cr.ProjectId == changeRequest.ProjectId && cr.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                if (existing.ChangeRequestId != changeRequestId)
                {
                    throw new DomainException("Idempotency key already used for another change request.", StatusCodes.Status409Conflict);
                }

                return existing;
            }
        }

        EnsureChangeRequestRowVersion(changeRequest, rowVersion);

        var previousStatus = changeRequest.Status;
        if (previousStatus == ChangeRequestStatus.Rejected)
        {
            return changeRequest;
        }

        if (previousStatus == ChangeRequestStatus.Applied)
        {
            throw new DomainException("Applied change requests cannot be rejected.", StatusCodes.Status409Conflict);
        }

        var now = DateTimeOffset.UtcNow;
        changeRequest.Status = ChangeRequestStatus.Rejected;
        changeRequest.DecidedBy = actorId;
        changeRequest.DecidedAt = now;
        changeRequest.ClientRequestId = clientRequestId;

        if (changeRequest.Project is not null)
        {
            changeRequest.Project.UpdatedAt = now;

            await _db.StatusHistory.AddAsync(new StatusHistory
            {
                ProjectId = changeRequest.Project.ProjectId,
                FromStatus = changeRequest.Project.Status,
                ToStatus = changeRequest.Project.Status,
                ChangedBy = actorId,
                ChangedAt = now,
                Note = $"CR rejected: {changeRequest.Title}"
            }, cancellationToken);
        }

        EnqueueEvent(
            topic: "project.change-request.rejected",
            projectId: changeRequest.ProjectId,
            quoteId: null,
            changeRequestId: changeRequest.ChangeRequestId,
            fromStatus: previousStatus.ToString(),
            toStatus: ChangeRequestStatus.Rejected.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { changeRequest.Title },
            deltas: new
            {
                changeRequest.ProposedPriceDelta,
                changeRequest.ProposedExtraHours,
                changeRequest.ProposedNewDueDate
            });

        await _db.SaveChangesAsync(cancellationToken);
        return changeRequest;
    }

    public async Task<ChangeRequest> ApplyChangeRequestAsync(Guid changeRequestId, uint rowVersion, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken)
    {
        var changeRequest = await _db.ChangeRequests
            .Include(cr => cr.Project)
            .FirstOrDefaultAsync(cr => cr.ChangeRequestId == changeRequestId, cancellationToken);

        if (changeRequest is null)
        {
            throw new DomainException("Change request not found.", StatusCodes.Status404NotFound);
        }

        clientRequestId = NormalizeClientRequestId(clientRequestId);
        if (!string.IsNullOrEmpty(clientRequestId))
        {
            var existing = await _db.ChangeRequests.AsNoTracking()
                .FirstOrDefaultAsync(cr => cr.ProjectId == changeRequest.ProjectId && cr.ClientRequestId == clientRequestId, cancellationToken);
            if (existing is not null)
            {
                if (existing.ChangeRequestId != changeRequestId)
                {
                    throw new DomainException("Idempotency key already used for another change request.", StatusCodes.Status409Conflict);
                }

                return existing;
            }
        }

        EnsureChangeRequestRowVersion(changeRequest, rowVersion);

        var previousStatus = changeRequest.Status;
        if (previousStatus != ChangeRequestStatus.Approved)
        {
            if (previousStatus == ChangeRequestStatus.Applied)
            {
                return changeRequest;
            }

            throw new DomainException("Only approved change requests can be applied.", StatusCodes.Status409Conflict);
        }

        if (changeRequest.Project is null)
        {
            throw new DomainException("Change request project data is unavailable.");
        }

        var now = DateTimeOffset.UtcNow;
        changeRequest.Status = ChangeRequestStatus.Applied;
        changeRequest.ClientRequestId = clientRequestId;
        changeRequest.Project.UpdatedAt = now;

        if (changeRequest.ProposedPriceDelta.HasValue)
        {
            changeRequest.Project.Budget += changeRequest.ProposedPriceDelta.Value;
        }

        if (changeRequest.ProposedNewDueDate.HasValue)
        {
            changeRequest.Project.DueDate = changeRequest.ProposedNewDueDate;
        }

        if (changeRequest.ProposedExtraHours.HasValue && changeRequest.ProposedExtraHours.Value > 0)
        {
            _db.Tasks.Add(new TaskItem
            {
                TaskId = Guid.NewGuid(),
                ProjectId = changeRequest.ProjectId,
                Title = $"CR: {changeRequest.Title} extra hours",
                EstimateHours = changeRequest.ProposedExtraHours.Value,
                Status = "Pending"
            });
        }

        await _db.StatusHistory.AddAsync(new StatusHistory
        {
            ProjectId = changeRequest.Project.ProjectId,
            FromStatus = changeRequest.Project.Status,
            ToStatus = changeRequest.Project.Status,
            ChangedBy = actorId,
            ChangedAt = now,
            Note = $"CR applied: {changeRequest.Title}"
        }, cancellationToken);

        EnqueueEvent(
            topic: "project.change-request.applied",
            projectId: changeRequest.ProjectId,
            quoteId: null,
            changeRequestId: changeRequest.ChangeRequestId,
            fromStatus: previousStatus.ToString(),
            toStatus: ChangeRequestStatus.Applied.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { changeRequest.Title },
            deltas: new
            {
                changeRequest.ProposedPriceDelta,
                changeRequest.ProposedExtraHours,
                changeRequest.ProposedNewDueDate
            });

        EnqueueEvent(
            topic: "project.updated",
            projectId: changeRequest.Project.ProjectId,
            quoteId: null,
            changeRequestId: changeRequest.ChangeRequestId,
            fromStatus: changeRequest.Project.Status.ToString(),
            toStatus: changeRequest.Project.Status.ToString(),
            actor: new ActorContext(actorId, actorRole),
            occurredAt: now,
            metadata: new { changeRequest.Project.CustomerId, changeRequest.Project.Title, changeRequest.Project.Budget, changeRequest.Project.DueDate });

        await _db.SaveChangesAsync(cancellationToken);
        return changeRequest;
    }

    private static string? NormalizeClientRequestId(string? value) => string.IsNullOrWhiteSpace(value) ? null : value.Trim();

    private static bool IsEarlierThan(ProjectStatus current, ProjectStatus comparison) =>
        ProjectStatusOrder.TryGetValue(current, out var currentOrder)
        && ProjectStatusOrder.TryGetValue(comparison, out var comparisonOrder)
        && currentOrder < comparisonOrder;

    private static void EnsureChangeRequestRowVersion(ChangeRequest changeRequest, uint rowVersion)
    {
        if (changeRequest.xmin == 0)
        {
            return;
        }

        if (rowVersion == 0 || changeRequest.xmin != rowVersion)
        {
            throw new DomainException("Change request has changed. Refresh and retry.", StatusCodes.Status409Conflict);
        }
    }

    private void EnqueueEvent(
        string topic,
        Guid projectId,
        Guid? quoteId,
        Guid? changeRequestId,
        string? fromStatus,
        string? toStatus,
        ActorContext actor,
        DateTimeOffset occurredAt,
        object? metadata = null,
        object? deltas = null)
    {
        var payload = new
        {
            projectId,
            quoteId,
            changeRequestId,
            fromStatus,
            toStatus,
            changedBy = new
            {
                userId = actor.Id,
                role = actor.Role ?? "system"
            },
            occurredAt,
            metadata,
            deltas
        };

        _db.Outbox.Add(new OutboxMessage
        {
            Id = Guid.NewGuid(),
            Topic = topic,
            Payload = JsonSerializer.Serialize(payload, _serializerOptions),
            CreatedAt = occurredAt
        });
    }

    private readonly record struct ActorContext(Guid Id, string Role);
}
