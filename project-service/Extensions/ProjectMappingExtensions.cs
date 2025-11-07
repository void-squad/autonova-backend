using ProjectService.Domain.Entities;
using ProjectService.Dtos;

namespace ProjectService.Extensions;

public static class ProjectMappingExtensions
{
    public static ProjectResponse ToResponse(this Project project)
    {
        var tasks = project.Tasks?.Select(t => new ProjectResponse.TaskResponse
        {
            TaskId = t.TaskId,
            Title = t.Title,
            EstimateHours = t.EstimateHours,
            AssigneeId = t.AssigneeId,
            Status = t.Status
        }).ToArray() ?? Array.Empty<ProjectResponse.TaskResponse>();

        var quotes = project.Quotes?.Select(q => new ProjectResponse.QuoteResponse
        {
            QuoteId = q.QuoteId,
            Total = q.Total,
            Status = q.Status,
            IssuedAt = q.IssuedAt,
            ApprovedAt = q.ApprovedAt,
            RejectedAt = q.RejectedAt,
            ApprovedBy = q.ApprovedBy,
            RejectedBy = q.RejectedBy,
            ClientRequestId = q.ClientRequestId,
            xmin = q.xmin
        }).ToArray() ?? Array.Empty<ProjectResponse.QuoteResponse>();

        var history = project.StatusHistory?.OrderByDescending(h => h.ChangedAt).Select(h => new ProjectResponse.StatusHistoryResponse
        {
            Id = h.Id,
            FromStatus = h.FromStatus,
            ToStatus = h.ToStatus,
            ChangedBy = h.ChangedBy,
            ChangedAt = h.ChangedAt,
            Note = h.Note
        }).ToArray() ?? Array.Empty<ProjectResponse.StatusHistoryResponse>();

        return new ProjectResponse
        {
            ProjectId = project.ProjectId,
            CustomerId = project.CustomerId,
            Title = project.Title,
            Status = project.Status,
            CreatedAt = project.CreatedAt,
            UpdatedAt = project.UpdatedAt,
            Budget = project.Budget,
            DueDate = project.DueDate,
            Tasks = tasks,
            Quotes = quotes,
            StatusHistory = history
        };
    }
}
