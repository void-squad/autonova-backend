namespace ProjectService.Dtos;

public class CreateProjectRequest
{
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public DateTimeOffset? RequestedStart { get; set; }
    public DateTimeOffset? RequestedEnd { get; set; }
    public Guid? AppointmentId { get; set; }
    public string? AppointmentSnapshot { get; set; }
    public List<CreateProjectTaskRequest> Tasks { get; set; } = new();
}
