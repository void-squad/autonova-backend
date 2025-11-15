using TaskStatus = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Domain.Entities;

public class ProjectTask
{
    public Guid TaskId { get; set; }
    public Guid ProjectId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string ServiceType { get; set; } = string.Empty;
    public string? Detail { get; set; }
    public TaskStatus Status { get; set; } = TaskStatus.Requested;
    public long? AssigneeId { get; set; }
    public decimal? EstimateHours { get; set; }
    public DateTimeOffset? ScheduledStart { get; set; }
    public DateTimeOffset? ScheduledEnd { get; set; }
    public Guid? AppointmentId { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public Project Project { get; set; } = null!;
    public ICollection<ProjectActivity> Activity { get; set; } = new List<ProjectActivity>();
}
