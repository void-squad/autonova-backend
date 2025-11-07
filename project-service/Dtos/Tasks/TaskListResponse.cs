namespace ProjectService.Dtos.Tasks;

public class TaskListResponse
{
    public int Page { get; set; }
    public int PageSize { get; set; }
    public int Total { get; set; }
    public List<TaskResponse> Items { get; set; } = new();
}
