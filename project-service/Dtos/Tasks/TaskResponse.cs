namespace ProjectService.Dtos.Tasks;

public class TaskResponse
{
    public Guid TaskId { get; set; }
    public Guid ProjectId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public Guid? AssigneeId { get; set; }
    public string Status { get; set; } = string.Empty;
    public decimal EstimateHours { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public TaskProjectSummary? Project { get; set; }
}

public class TaskProjectSummary
{
    public Guid ProjectId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
}
