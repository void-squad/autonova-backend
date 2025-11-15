using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;
using ProjectService.Extensions;
using TaskStatusEnum = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/admin/projects")]
[Authorize(Policy = "AdminOnly")]
public class AdminProjectsController : ControllerBase
{
    private readonly AppDb _db;
    private readonly IValidator<ApproveProjectRequest> _approveValidator;
    private readonly IValidator<CreateProjectTaskRequest> _taskValidator;

    public AdminProjectsController(
        AppDb db,
        IValidator<ApproveProjectRequest> approveValidator,
        IValidator<CreateProjectTaskRequest> taskValidator)
    {
        _db = db;
        _approveValidator = approveValidator;
        _taskValidator = taskValidator;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<ProjectSummaryDto>>> GetProjects(
        [FromQuery] ProjectStatus? status,
        CancellationToken cancellationToken)
    {
        var query = _db.Projects.AsNoTracking();
        if (status.HasValue)
        {
            query = query.Where(p => p.Status == status.Value);
        }

        var projects = await query
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync(cancellationToken);

        return Ok(projects.Select(p => p.ToSummary()));
    }

    [HttpGet("customer/{customerId:guid}")]
    public async Task<ActionResult<IEnumerable<ProjectSummaryDto>>> GetProjectsByCustomer(
        Guid customerId,
        CancellationToken cancellationToken)
    {
        var projects = await _db.Projects
            .AsNoTracking()
            .Where(p => p.CustomerId == customerId)
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync(cancellationToken);

        return Ok(projects.Select(p => p.ToSummary()));
    }

    [HttpGet("{projectId:guid}")]
    public async Task<ActionResult<ProjectDetailsDto>> GetProject(Guid projectId, CancellationToken cancellationToken)
    {
        var project = await _db.Projects
            .Include(p => p.Tasks)
            .Include(p => p.Activity)
            .FirstOrDefaultAsync(p => p.ProjectId == projectId, cancellationToken);

        if (project is null)
        {
            return NotFound();
        }

        return Ok(project.ToDetails());
    }

    [HttpPost("{projectId:guid}/approve")]
    public async Task<IActionResult> ApproveProject(Guid projectId, [FromBody] ApproveProjectRequest request, CancellationToken cancellationToken)
    {
        var validation = await _approveValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            return ValidationProblem(new ValidationProblemDetails(validation.ToDictionary()));
        }

        var project = await _db.Projects
            .Include(p => p.Tasks)
            .FirstOrDefaultAsync(p => p.ProjectId == projectId, cancellationToken);
        if (project is null)
        {
            return NotFound();
        }

        project.Status = ProjectStatus.Approved;
        project.ApprovedStart = request.ApprovedStart;
        project.ApprovedEnd = request.ApprovedEnd;
        project.UpdatedAt = DateTimeOffset.UtcNow;
        var actorId = User.GetUserId();

        foreach (var update in request.Tasks)
        {
            var task = project.Tasks.FirstOrDefault(t => t.TaskId == update.TaskId);
            if (task is null)
            {
                continue;
            }

            task.AssigneeId = update.AssigneeId;
            task.ScheduledStart = update.ScheduledStart;
            task.ScheduledEnd = update.ScheduledEnd;
            task.Status = update.AssigneeId.HasValue ? TaskStatusEnum.Accepted : TaskStatusEnum.Requested;
            task.UpdatedAt = DateTimeOffset.UtcNow;

            _db.Activity.Add(new ProjectActivity
            {
                ProjectId = project.ProjectId,
                TaskId = task.TaskId,
                ActorId = actorId,
                ActorRole = "admin",
                Message = $"Task updated by admin (assignee: {(update.AssigneeId.HasValue ? update.AssigneeId : "unassigned")})",
                CreatedAt = DateTimeOffset.UtcNow
            });
        }

        _db.Activity.Add(new ProjectActivity
        {
            ProjectId = project.ProjectId,
            ActorId = actorId,
            ActorRole = "admin",
            Message = "Project approved",
            CreatedAt = DateTimeOffset.UtcNow
        });

        await _db.SaveChangesAsync(cancellationToken);
        return NoContent();
    }

    [HttpPost("{projectId:guid}/tasks")]
    public async Task<ActionResult<ProjectTaskDto>> AddTask(Guid projectId, [FromBody] CreateProjectTaskRequest request, CancellationToken cancellationToken)
    {
        var validation = await _taskValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            return ValidationProblem(new ValidationProblemDetails(validation.ToDictionary()));
        }

        var project = await _db.Projects
            .FirstOrDefaultAsync(p => p.ProjectId == projectId, cancellationToken);

        if (project is null)
        {
            return NotFound();
        }

        var now = DateTimeOffset.UtcNow;
        var task = new ProjectTask
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
        _db.Activity.Add(new ProjectActivity
        {
            ProjectId = projectId,
            TaskId = task.TaskId,
            ActorId = User.GetUserId(),
            ActorRole = "admin",
            Message = "Task created by admin",
            CreatedAt = now
        });

        await _db.SaveChangesAsync(cancellationToken);

        return CreatedAtAction(nameof(GetProject), new { projectId }, task.ToDto());
    }
}
