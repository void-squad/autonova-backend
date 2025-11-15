using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Dtos.Tasks;
using ProjectService.Mapping;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Policy = "EmployeeAccess")]
[ProducesResponseType(typeof(TaskListResponse), StatusCodes.Status200OK)]
public class TasksController : ControllerBase
{
    private readonly AppDb _db;

    public TasksController(AppDb db)
    {
        _db = db;
    }

    [HttpGet("employee/{assigneeId:long}")]
    [ProducesResponseType(typeof(IEnumerable<TaskAssignmentResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<TaskAssignmentResponse>>> GetTasksForEmployee(long assigneeId, CancellationToken cancellationToken = default)
    {
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

        return Ok(task.ToAssignmentResponse());
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<ActionResult<TaskListResponse>> Get(
        [FromQuery] long? assigneeId,
        [FromQuery] string? status,
        [FromQuery] Guid? projectId,
        [FromQuery] bool includeProject = false,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50,
        CancellationToken cancellationToken = default)
    {
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
