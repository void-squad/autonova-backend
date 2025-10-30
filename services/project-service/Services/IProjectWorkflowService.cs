using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;

namespace ProjectService.Services;

public interface IProjectWorkflowService
{
    Task<Project> CreateProjectAsync(CreateProjectRequest request, Guid actorId, CancellationToken cancellationToken);
    Task<Project?> GetProjectAsync(Guid projectId, CancellationToken cancellationToken);
    Task<IReadOnlyList<Project>> GetProjectsAsync(ProjectStatus? status, Guid? customerId, CancellationToken cancellationToken);
    Task<Project> UpdateStatusAsync(Guid projectId, ProjectStatus newStatus, Guid actorId, CancellationToken cancellationToken);
    Task<Quote> CreateQuoteAsync(Guid projectId, CreateQuoteRequest request, Guid actorId, CancellationToken cancellationToken);
    Task<Quote> ApproveQuoteAsync(Guid quoteId, Guid actorId, CancellationToken cancellationToken);
    Task<Quote> RejectQuoteAsync(Guid quoteId, Guid actorId, CancellationToken cancellationToken);
}
