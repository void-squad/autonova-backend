using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;
using ProjectService.Extensions;
using TaskStatusEnum = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/tasks")]
public class TasksController : ControllerBase
{
    private readonly AppDb _db;
    private readonly IValidator<UpdateTaskStatusRequest> _statusValidator;
    private readonly IValidator<CreateProjectTaskRequest> _taskValidator;

    public TasksController(
        AppDb db,
        IValidator<UpdateTaskStatusRequest> statusValidator,
        IValidator<CreateProjectTaskRequest> taskValidator)
    {
        _db = db;
        _statusValidator = statusValidator;
        _taskValidator = taskValidator;
    }

    [HttpGet("assigned")]
    [Authorize(Policy = "EmployeeOrAdmin")]
    public async Task<ActionResult<IEnumerable<ProjectTaskDto>>> GetAssignedTasks(CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        var isAdmin = User.IsInRole("ADMIN");

        var query = _db.Tasks.AsNoTracking();
        if (!isAdmin)
        {
            query = query.Where(t => t.AssigneeId == userId);
        }

        var tasks = await query
            .OrderBy(t => t.Status)
            .ThenByDescending(t => t.CreatedAt)
            .ToListAsync(cancellationToken);

        return Ok(tasks.Select(t => t.ToDto()));
    }

    [HttpPatch("{taskId:guid}/status")]
    [Authorize(Policy = "EmployeeOrAdmin")]
    public async Task<IActionResult> UpdateStatus(Guid taskId, [FromBody] UpdateTaskStatusRequest request, CancellationToken cancellationToken)
    {
        var validation = await _statusValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            return ValidationProblem(new ValidationProblemDetails(validation.ToDictionary()));
        }

        var isAdmin = User.IsInRole("ADMIN");
        var userId = User.GetUserId();

        var task = await _db.Tasks.FirstOrDefaultAsync(t => t.TaskId == taskId, cancellationToken);
        if (task is null)
        {
            return NotFound();
        }

        if (!isAdmin && task.AssigneeId != userId)
        {
            return Forbid();
        }

        if (!isAdmin && request.Status == TaskStatusEnum.Cancelled)
        {
            return BadRequest("Only administrators can cancel tasks.");
        }

        task.Status = request.Status;
        task.UpdatedAt = DateTimeOffset.UtcNow;

        _db.Activity.Add(new Domain.Entities.ProjectActivity
        {
            ProjectId = task.ProjectId,
            TaskId = task.TaskId,
            ActorId = userId,
            ActorRole = User.GetPrimaryRole() ?? "system",
            Message = $"Task marked as {request.Status}",
            CreatedAt = DateTimeOffset.UtcNow
        });

        await _db.SaveChangesAsync(cancellationToken);
        return NoContent();
    }

    [HttpPost("{projectId:guid}")]
    [Authorize(Policy = "AdminOnly")]
    public async Task<ActionResult<ProjectTaskDto>> CreateTask(Guid projectId, [FromBody] CreateProjectTaskRequest request, CancellationToken cancellationToken)
    {
        var validation = await _taskValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            return ValidationProblem(new ValidationProblemDetails(validation.ToDictionary()));
        }

        var projectExists = await _db.Projects.AnyAsync(p => p.ProjectId == projectId, cancellationToken);
        if (!projectExists)
        {
            return NotFound();
        }

        var now = DateTimeOffset.UtcNow;
        var task = new Domain.Entities.ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = projectId,
            Title = request.Title.Trim(),
            ServiceType = request.ServiceType.Trim(),
            Detail = request.Detail?.Trim(),
            ScheduledStart = request.ScheduledStart,
            ScheduledEnd = request.ScheduledEnd,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _db.Tasks.AddAsync(task, cancellationToken);
        await _db.SaveChangesAsync(cancellationToken);

        return CreatedAtAction(nameof(GetAssignedTasks), new { }, task.ToDto());
    }

    [HttpGet("by-assignee/{assigneeId:long}")]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<ProjectTaskDto>>> GetTasksByAssignee(long assigneeId, CancellationToken cancellationToken)
    {
        var tasks = await _db.Tasks.AsNoTracking()
            .Where(t => t.AssigneeId == assigneeId)
            .OrderBy(t => t.Status)
            .ThenByDescending(t => t.CreatedAt)
            .ToListAsync(cancellationToken);

        return Ok(tasks.Select(t => t.ToDto()));
    }
}
