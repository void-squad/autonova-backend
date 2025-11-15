namespace ProjectService.Domain.Entities;

public class ProjectActivity
{
    public long Id { get; set; }
    public Guid ProjectId { get; set; }
    public Guid? TaskId { get; set; }
    public Guid ActorId { get; set; }
    public string ActorRole { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    public Project Project { get; set; } = null!;
    public ProjectTask? Task { get; set; }
}
