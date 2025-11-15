using ProjectService.Domain.Enums;

namespace ProjectService.Domain.Entities;

public class Project
{
    public Guid ProjectId { get; set; }
    public long CustomerId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public ProjectStatus Status { get; set; } = ProjectStatus.PendingReview;
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset? RequestedStart { get; set; }
    public DateTimeOffset? RequestedEnd { get; set; }
    public DateTimeOffset? ApprovedStart { get; set; }
    public DateTimeOffset? ApprovedEnd { get; set; }
    public long CreatedBy { get; set; }
    public Guid? AppointmentId { get; set; }
    public string? AppointmentSnapshot { get; set; }

    public ICollection<ProjectTask> Tasks { get; set; } = new List<ProjectTask>();
    public ICollection<ProjectActivity> Activity { get; set; } = new List<ProjectActivity>();
}
