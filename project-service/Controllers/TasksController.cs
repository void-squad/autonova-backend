using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Dtos.Tasks;
using ProjectService.Mapping;
using ProjectService.Security;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Policy = "EmployeeOrManager")]
[ProducesResponseType(typeof(TaskListResponse), StatusCodes.Status200OK)]
public class TasksController : ControllerBase
{
    private readonly AppDb _db;

    public TasksController(AppDb db)
    {
        _db = db;
    }

    [HttpGet("employee/{assigneeId:guid}")]
    [ProducesResponseType(typeof(IEnumerable<TaskAssignmentResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<TaskAssignmentResponse>>> GetTasksForEmployee(Guid assigneeId, CancellationToken cancellationToken = default)
    {
        if (UserContext.IsEmployee(User) && !UserContext.IsManager(User))
        {
            var userId = UserContext.UserId(User);
            if (!userId.HasValue || userId.Value != assigneeId)
            {
                return Forbid();
            }
        }

        var tasks = await _db.Tasks
            .AsNoTracking()
            .Where(t => t.AssigneeId == assigneeId)
            .OrderBy(t => t.Title)
            .ToListAsync(cancellationToken);

        return Ok(tasks.Select(t => t.ToAssignmentResponse()));
    }

    [HttpGet("project/{projectId:guid}")]
    [ProducesResponseType(typeof(IEnumerable<TaskAssignmentResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<TaskAssignmentResponse>>> GetTasksByProject(Guid projectId, CancellationToken cancellationToken = default)
    {
        var tasks = await _db.Tasks
            .AsNoTracking()
            .Where(t => t.ProjectId == projectId)
            .OrderBy(t => t.Title)
            .ToListAsync(cancellationToken);

        return Ok(tasks.Select(t => t.ToAssignmentResponse()));
    }

    [HttpGet("{taskId:guid}")]
    [ProducesResponseType(typeof(TaskAssignmentResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<TaskAssignmentResponse>> GetTaskById(Guid taskId, CancellationToken cancellationToken = default)
    {
        var task = await _db.Tasks
            .AsNoTracking()
            .FirstOrDefaultAsync(t => t.TaskId == taskId, cancellationToken);

        if (task is null)
        {
            return NotFound();
        }

        if (UserContext.IsEmployee(User) && !UserContext.IsManager(User))
        {
            var userId = UserContext.UserId(User);
            if (!userId.HasValue || task.AssigneeId != userId.Value)
            {
                return Forbid();
            }
        }

        return Ok(task.ToAssignmentResponse());
    }

    [HttpGet]
    public async Task<ActionResult<TaskListResponse>> Get(
        [FromQuery] Guid? assigneeId,
        [FromQuery] string? status,
        [FromQuery] Guid? projectId,
        [FromQuery] bool includeProject = false,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50,
        CancellationToken cancellationToken = default)
    {
        if (!assigneeId.HasValue && !UserContext.IsManager(User))
        {
            return BadRequest("assigneeId is required unless you have manager role.");
        }

        if (assigneeId.HasValue && UserContext.IsEmployee(User) && !UserContext.IsManager(User))
        {
            var userId = UserContext.UserId(User);
            if (!userId.HasValue || userId.Value != assigneeId.Value)
            {
                return Forbid();
            }
        }

        page = Math.Max(1, page);
        pageSize = Math.Clamp(pageSize, 1, 200);

        var query = _db.Tasks.AsNoTracking().AsQueryable();

        if (includeProject)
        {
            query = query.Include(t => t.Project);
        }

        if (assigneeId.HasValue)
        {
            query = query.Where(t => t.AssigneeId == assigneeId.Value);
        }

        if (projectId.HasValue)
        {
            query = query.Where(t => t.ProjectId == projectId.Value);
        }

        if (!string.IsNullOrWhiteSpace(status))
        {
            query = query.Where(t => t.Status == status);
        }

        var total = await query.CountAsync(cancellationToken);

        var tasks = await query
            .OrderByDescending(t => t.TaskId)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(cancellationToken);

        var response = new TaskListResponse
        {
            Page = page,
            PageSize = pageSize,
            Total = total,
            Items = tasks.Select(t => t.ToDto(includeProject)).ToList()
        };

        return Ok(response);
    }
}
