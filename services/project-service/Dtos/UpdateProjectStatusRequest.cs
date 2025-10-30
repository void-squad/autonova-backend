using ProjectService.Domain.Enums;

namespace ProjectService.Dtos;

public class UpdateProjectStatusRequest
{
    public ProjectStatus NewStatus { get; set; }
}
