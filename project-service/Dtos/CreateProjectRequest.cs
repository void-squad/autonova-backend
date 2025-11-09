namespace ProjectService.Dtos;

public class CreateProjectRequest
{
    public Guid CustomerId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
}
