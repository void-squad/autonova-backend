using ProjectService.Domain.Entities;
using ProjectService.Dtos;

namespace ProjectService.Extensions;

public static class ProjectMappingExtensions
{
    public static ProjectSummaryDto ToSummary(this Project project)
    {
        return new ProjectSummaryDto
        {
            ProjectId = project.ProjectId,
            VehicleId = project.VehicleId,
            Title = project.Title,
            Status = project.Status,
            CreatedAt = project.CreatedAt,
            UpdatedAt = project.UpdatedAt,
            RequestedStart = project.RequestedStart,
            RequestedEnd = project.RequestedEnd,
            ApprovedStart = project.ApprovedStart,
            ApprovedEnd = project.ApprovedEnd
        };
    }

    public static ProjectDetailsDto ToDetails(this Project project)
    {
        return new ProjectDetailsDto
        {
            ProjectId = project.ProjectId,
            CustomerId = project.CustomerId,
            VehicleId = project.VehicleId,
            Title = project.Title,
            Description = project.Description,
            Status = project.Status,
            CreatedAt = project.CreatedAt,
            UpdatedAt = project.UpdatedAt,
            RequestedStart = project.RequestedStart,
            RequestedEnd = project.RequestedEnd,
            ApprovedStart = project.ApprovedStart,
            ApprovedEnd = project.ApprovedEnd,
            AppointmentId = project.AppointmentId,
            AppointmentSnapshot = project.AppointmentSnapshot,
            Tasks = project.Tasks
                .OrderBy(t => t.CreatedAt)
                .Select(t => t.ToDto())
                .ToArray(),
            Activity = project.Activity
                .OrderByDescending(a => a.CreatedAt)
                .Select(a => a.ToDto())
                .ToArray()
        };
    }

    public static ProjectTaskDto ToDto(this ProjectTask task)
    {
        return new ProjectTaskDto
        {
            TaskId = task.TaskId,
            Title = task.Title,
            ServiceType = task.ServiceType,
            Detail = task.Detail,
            Status = task.Status,
            AssigneeId = task.AssigneeId,
            EstimateHours = task.EstimateHours,
            ScheduledStart = task.ScheduledStart,
            ScheduledEnd = task.ScheduledEnd,
            AppointmentId = task.AppointmentId,
            CreatedAt = task.CreatedAt,
            UpdatedAt = task.UpdatedAt
        };
    }

    public static ProjectActivityDto ToDto(this ProjectActivity activity)
    {
        return new ProjectActivityDto
        {
            Id = activity.Id,
            TaskId = activity.TaskId,
            ActorId = activity.ActorId,
            ActorRole = activity.ActorRole,
            Message = activity.Message,
            CreatedAt = activity.CreatedAt
        };
    }
}
