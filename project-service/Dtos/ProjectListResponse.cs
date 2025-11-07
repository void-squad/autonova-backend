namespace ProjectService.Dtos;

public class ProjectListResponse
{
    public int Page { get; set; }
    public int PageSize { get; set; }
    public int Total { get; set; }
    public List<ProjectResponse> Items { get; set; } = new();
}
