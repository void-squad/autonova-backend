using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;
using ProjectService.Dtos.ChangeRequests;

namespace ProjectService.Services;

public interface IProjectWorkflowService
{
    Task<Project> CreateProjectAsync(CreateProjectRequest request, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<Project?> GetProjectAsync(Guid projectId, CancellationToken cancellationToken);
    Task<IReadOnlyList<Project>> GetProjectsAsync(ProjectStatus? status, Guid? customerId, CancellationToken cancellationToken);
    Task<Project> UpdateStatusAsync(Guid projectId, ProjectStatus newStatus, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<Quote> CreateQuoteAsync(Guid projectId, CreateQuoteRequest request, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<Quote> ApproveQuoteAsync(Guid quoteId, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<Quote> RejectQuoteAsync(Guid quoteId, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<IReadOnlyList<StatusHistory>> GetStatusHistoryAsync(Guid projectId, CancellationToken cancellationToken);
    Task<ChangeRequest> CreateChangeRequestAsync(Guid projectId, CreateChangeRequestRequest request, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<IReadOnlyList<ChangeRequest>> GetProjectChangeRequestsAsync(Guid projectId, CancellationToken cancellationToken);
    Task<ChangeRequest?> GetChangeRequestAsync(Guid changeRequestId, CancellationToken cancellationToken);
    Task<ChangeRequest> ApproveChangeRequestAsync(Guid changeRequestId, uint rowVersion, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<ChangeRequest> RejectChangeRequestAsync(Guid changeRequestId, uint rowVersion, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
    Task<ChangeRequest> ApplyChangeRequestAsync(Guid changeRequestId, uint rowVersion, Guid actorId, string actorRole, string? clientRequestId, CancellationToken cancellationToken);
}
