using ProjectService.Domain.Enums;

namespace ProjectService.Dtos;

public class EmployeeProjectResponse
{
    public long Id { get; set; }
    public Guid ProjectId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public ProjectStatus Status { get; set; }
    public DateOnly? DueDate { get; set; }
}
