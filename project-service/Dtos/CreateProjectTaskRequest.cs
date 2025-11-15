namespace ProjectService.Dtos;

public class CreateProjectTaskRequest
{
    public string Title { get; set; } = string.Empty;
    public string ServiceType { get; set; } = string.Empty;
    public string? Detail { get; set; }
    public DateTimeOffset? ScheduledStart { get; set; }
    public DateTimeOffset? ScheduledEnd { get; set; }
}
