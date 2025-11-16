using System.ComponentModel.DataAnnotations;

namespace ProjectService.Dtos;

public class UpdateAppointmentStatusRequest
{
    [Required]
    public string Status { get; set; } = string.Empty;

    public string? AdminNote { get; set; }
}
