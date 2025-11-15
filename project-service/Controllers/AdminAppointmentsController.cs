using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/admin/appointments")]
[Authorize(Policy = "AdminOnly")]
public class AdminAppointmentsController : ControllerBase
{
    [HttpGet]
    public IActionResult GetAppointments()
    {
        // Placeholder response until appointment service integration is available.
        return Ok(Array.Empty<object>());
    }

    [HttpPost("import")]
    public IActionResult ImportAppointment([FromBody] object payload)
    {
        // Store payload for future processing; currently this is a stub.
        return Accepted(new { message = "Appointment payload received for processing." });
    }
}
