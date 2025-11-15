using TaskStatus = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Dtos;

public class ProjectTaskDto
{
    public Guid TaskId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string ServiceType { get; set; } = string.Empty;
    public string? Detail { get; set; }
    public TaskStatus Status { get; set; }
    public long? AssigneeId { get; set; }
    public decimal? EstimateHours { get; set; }
    public DateTimeOffset? ScheduledStart { get; set; }
    public DateTimeOffset? ScheduledEnd { get; set; }
    public Guid? AppointmentId { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
}
