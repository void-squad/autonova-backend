using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ProjectService.Dtos;
using ProjectService.Services;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/admin/appointments")]
[Authorize(Policy = "AdminOnly")]
public class AdminAppointmentsController : ControllerBase
{
    private readonly IAppointmentServiceClient _appointmentClient;

    public AdminAppointmentsController(IAppointmentServiceClient appointmentClient)
    {
        _appointmentClient = appointmentClient;
    }

    [HttpGet]
    public async Task<IActionResult> GetAppointments(
        [FromQuery] string? status,
        [FromQuery] DateTimeOffset? from,
        [FromQuery] DateTimeOffset? to,
        CancellationToken cancellationToken)
    {
        var items = await _appointmentClient.GetAdminAppointmentsAsync(status, from, to, cancellationToken);
        return Ok(items);
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> GetAppointment(Guid id, CancellationToken cancellationToken)
    {
        var appointment = await _appointmentClient.GetAppointmentAsync(id, cancellationToken);
        return appointment is null ? NotFound() : Ok(appointment);
    }

    [HttpPatch("{id:guid}/status")]
    public async Task<IActionResult> UpdateStatus(Guid id, [FromBody] UpdateAppointmentStatusRequest request, CancellationToken cancellationToken)
    {
        await _appointmentClient.UpdateAppointmentStatusAsync(id, request, cancellationToken);
        return NoContent();
    }
}
