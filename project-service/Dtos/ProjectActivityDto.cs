namespace ProjectService.Dtos;

public class ProjectActivityDto
{
    public long Id { get; set; }
    public Guid? TaskId { get; set; }
    public long ActorId { get; set; }
    public string ActorRole { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; }
}
