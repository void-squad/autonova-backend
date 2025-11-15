using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/debug")]
[Authorize]
[ApiExplorerSettings(IgnoreApi = true)]
public class DebugController : ControllerBase
{
    [HttpGet("whoami")]
    public ActionResult<object> WhoAmI()
    {
        return Ok(new
        {
            Name = User.Identity?.Name,
            Claims = User.Claims.Select(c => new { c.Type, c.Value })
        });
    }
}
