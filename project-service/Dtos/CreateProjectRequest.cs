namespace ProjectService.Dtos;

public class CreateProjectRequest
{
    public long CustomerId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
}
