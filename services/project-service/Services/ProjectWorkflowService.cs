using System.Text.Json;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Domain.Exceptions;
using ProjectService.Dtos;

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

    private readonly AppDb _db;
    private readonly ILogger<ProjectWorkflowService> _logger;
    private readonly JsonSerializerOptions _serializerOptions = new(JsonSerializerDefaults.Web);

    public ProjectWorkflowService(AppDb db, ILogger<ProjectWorkflowService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task<Project> CreateProjectAsync(CreateProjectRequest request, Guid actorId, CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        if (request.CustomerId == Guid.Empty)
        {
            throw new DomainException("CustomerId is required.");
        }

        var title = request.Title?.Trim();
        if (string.IsNullOrWhiteSpace(title))
        {
            throw new DomainException("Title is required.");
        }

        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = request.CustomerId,
            Title = title,
            Status = ProjectStatus.Requested,
            CreatedAt = now,
            UpdatedAt = now
        };

        var changedBy = actorId != Guid.Empty ? actorId : request.CustomerId;

        var history = new StatusHistory
        {
            ProjectId = project.ProjectId,
            FromStatus = ProjectStatus.Requested,
            ToStatus = ProjectStatus.Requested,
            ChangedBy = changedBy,
            ChangedAt = now
        };

        var outbox = new OutboxMessage
        {
            Id = Guid.NewGuid(),
            Topic = "project.created",
            Payload = JsonSerializer.Serialize(new
            {
                projectId = project.ProjectId,
                customerId = project.CustomerId,
                title = project.Title,
                status = project.Status.ToString(),
                occurredAt = now
            }, _serializerOptions),
            CreatedAt = now
        };

        await _db.Projects.AddAsync(project, cancellationToken);
        await _db.StatusHistory.AddAsync(history, cancellationToken);
        await _db.Outbox.AddAsync(outbox, cancellationToken);

        await _db.SaveChangesAsync(cancellationToken);
        return project;
    }

    public async Task<Project?> GetProjectAsync(Guid projectId, CancellationToken cancellationToken)
    {
        return await _db.Projects
            .Include(x => x.Tasks)
            .Include(x => x.Quotes)
            .Include(x => x.StatusHistory)
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

    public async Task<Project> UpdateStatusAsync(Guid projectId, ProjectStatus newStatus, Guid actorId, CancellationToken cancellationToken)
    {
        var project = await _db.Projects
            .Include(x => x.StatusHistory)
            .FirstOrDefaultAsync(x => x.ProjectId == projectId, cancellationToken);

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
            throw new DomainException($"Illegal transition from {project.Status} to {newStatus}.", StatusCodes.Status409Conflict);
        }

        var now = DateTimeOffset.UtcNow;
        var history = new StatusHistory
        {
            ProjectId = project.ProjectId,
            FromStatus = project.Status,
            ToStatus = newStatus,
            ChangedBy = actorId,
            ChangedAt = now
        };

        project.Status = newStatus;
        project.UpdatedAt = now;

        _db.StatusHistory.Add(history);
        _db.Outbox.Add(new OutboxMessage
        {
            Id = Guid.NewGuid(),
            Topic = "project.updated",
            Payload = JsonSerializer.Serialize(new
            {
                projectId = project.ProjectId,
                status = project.Status.ToString(),
                occurredAt = now
            }, _serializerOptions),
            CreatedAt = now
        });

        await _db.SaveChangesAsync(cancellationToken);
        return project;
    }

    public async Task<Quote> CreateQuoteAsync(Guid projectId, CreateQuoteRequest request, Guid actorId, CancellationToken cancellationToken)
    {
        var project = await _db.Projects
            .Include(x => x.Quotes)
            .FirstOrDefaultAsync(x => x.ProjectId == projectId, cancellationToken);

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
            IssuedAt = now
        };

        project.Quotes.Add(quote);
        project.UpdatedAt = now;

        _db.Outbox.Add(new OutboxMessage
        {
            Id = Guid.NewGuid(),
            Topic = "quote.draft.created",
            Payload = JsonSerializer.Serialize(new
            {
                quoteId = quote.QuoteId,
                projectId = projectId,
                total = quote.Total,
                status = quote.Status.ToString(),
                issuedAt = now
            }, _serializerOptions),
            CreatedAt = now
        });

        await _db.SaveChangesAsync(cancellationToken);
        return quote;
    }

    public async Task<Quote> ApproveQuoteAsync(Guid quoteId, Guid actorId, CancellationToken cancellationToken)
    {
        var quote = await _db.Quotes
            .Include(x => x.Project)
            .Include(x => x.Project!.StatusHistory)
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

        _db.Outbox.Add(new OutboxMessage
        {
            Id = Guid.NewGuid(),
            Topic = "quote.approved",
            Payload = JsonSerializer.Serialize(new
            {
                quoteId = quote.QuoteId,
                projectId = quote.ProjectId,
                approvedAt = now,
                approvedBy = actorId
            }, _serializerOptions),
            CreatedAt = now
        });

        if (quote.Project is not null && quote.Project.Status != ProjectStatus.Approved)
        {
            var previous = quote.Project.Status;
            quote.Project.Status = ProjectStatus.Approved;
            quote.Project.UpdatedAt = now;

            _db.StatusHistory.Add(new StatusHistory
            {
                ProjectId = quote.Project.ProjectId,
                FromStatus = previous,
                ToStatus = ProjectStatus.Approved,
                ChangedBy = actorId,
                ChangedAt = now
            });

            _db.Outbox.Add(new OutboxMessage
            {
                Id = Guid.NewGuid(),
                Topic = "project.updated",
                Payload = JsonSerializer.Serialize(new
                {
                    projectId = quote.Project.ProjectId,
                    status = quote.Project.Status.ToString(),
                    occurredAt = now
                }, _serializerOptions),
                CreatedAt = now
            });
        }

        await _db.SaveChangesAsync(cancellationToken);
        return quote;
    }

    public async Task<Quote> RejectQuoteAsync(Guid quoteId, Guid actorId, CancellationToken cancellationToken)
    {
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

        _db.Outbox.Add(new OutboxMessage
        {
            Id = Guid.NewGuid(),
            Topic = "quote.rejected",
            Payload = JsonSerializer.Serialize(new
            {
                quoteId = quote.QuoteId,
                projectId = quote.ProjectId,
                rejectedAt = now,
                rejectedBy = actorId
            }, _serializerOptions),
            CreatedAt = now
        });

        if (quote.Project is not null)
        {
            quote.Project.UpdatedAt = now;
        }

        await _db.SaveChangesAsync(cancellationToken);
        return quote;
    }
}
