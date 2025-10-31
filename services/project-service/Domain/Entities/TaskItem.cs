namespace ProjectService.Domain.Entities;

public class TaskItem
{
    public Guid TaskId { get; set; }
    public Guid ProjectId { get; set; }
    public string Title { get; set; } = string.Empty;
    public decimal EstimateHours { get; set; }
    public Guid? AssigneeId { get; set; }
    public string Status { get; set; } = string.Empty;

    public Project? Project { get; set; }
}
