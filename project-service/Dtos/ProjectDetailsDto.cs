using ProjectService.Domain.Enums;

namespace ProjectService.Dtos;

public class ProjectDetailsDto
{
    public Guid ProjectId { get; set; }
    public long CustomerId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public ProjectStatus Status { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public DateTimeOffset? RequestedStart { get; set; }
    public DateTimeOffset? RequestedEnd { get; set; }
    public DateTimeOffset? ApprovedStart { get; set; }
    public DateTimeOffset? ApprovedEnd { get; set; }
    public Guid? AppointmentId { get; set; }
    public string? AppointmentSnapshot { get; set; }
    public IReadOnlyCollection<ProjectTaskDto> Tasks { get; set; } = Array.Empty<ProjectTaskDto>();
    public IReadOnlyCollection<ProjectActivityDto> Activity { get; set; } = Array.Empty<ProjectActivityDto>();
}
