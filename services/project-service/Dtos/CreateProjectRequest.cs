namespace ProjectService.Dtos;

public class CreateProjectRequest
{
    public Guid CustomerId { get; set; }
    public string Title { get; set; } = string.Empty;
}
