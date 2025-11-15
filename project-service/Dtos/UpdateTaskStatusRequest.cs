using TaskStatus = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Dtos;

public class UpdateTaskStatusRequest
{
    public TaskStatus Status { get; set; }
    public string? Note { get; set; }
}
