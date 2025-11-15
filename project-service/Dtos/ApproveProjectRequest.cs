namespace ProjectService.Dtos;

public class ApproveProjectRequest
{
    public DateTimeOffset? ApprovedStart { get; set; }
    public DateTimeOffset? ApprovedEnd { get; set; }
    public List<ApproveProjectTaskUpdate> Tasks { get; set; } = new();
}

public class ApproveProjectTaskUpdate
{
    public Guid TaskId { get; set; }
    public long? AssigneeId { get; set; }
    public DateTimeOffset? ScheduledStart { get; set; }
    public DateTimeOffset? ScheduledEnd { get; set; }
}
