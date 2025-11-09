namespace ProjectService.Dtos.Tasks;

public class TaskAssignmentResponse
{
    public Guid TaskId { get; set; }
    public Guid ProjectId { get; set; }
    public string Title { get; set; } = string.Empty;
    public decimal EstimateHours { get; set; }
    public long? AssigneeId { get; set; }
    public string Status { get; set; } = string.Empty;
}
