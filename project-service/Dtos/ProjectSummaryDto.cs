using ProjectService.Domain.Enums;

namespace ProjectService.Dtos;

public class ProjectSummaryDto
{
    public Guid ProjectId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public ProjectStatus Status { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public DateTimeOffset? RequestedStart { get; set; }
    public DateTimeOffset? RequestedEnd { get; set; }
    public DateTimeOffset? ApprovedStart { get; set; }
    public DateTimeOffset? ApprovedEnd { get; set; }
}
