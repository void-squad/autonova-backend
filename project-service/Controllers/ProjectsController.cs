using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;
using ProjectService.Extensions;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ProjectsController : ControllerBase
{
    private readonly AppDb _db;
    private readonly IValidator<CreateProjectRequest> _createValidator;

    public ProjectsController(AppDb db, IValidator<CreateProjectRequest> createValidator)
    {
        _db = db;
        _createValidator = createValidator;
    }

    [HttpPost]
    [Authorize(Policy = "CustomerOnly")]
    public async Task<ActionResult<ProjectDetailsDto>> CreateProject(
        [FromBody] CreateProjectRequest request,
        CancellationToken cancellationToken)
    {
        var validation = await _createValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            return ValidationProblem(new ValidationProblemDetails(validation.ToDictionary()));
        }

        var customerId = User.GetUserId();
        var now = DateTimeOffset.UtcNow;
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = customerId,
            VehicleId = request.VehicleId,
            Title = request.Title.Trim(),
            Description = request.Description?.Trim(),
            RequestedStart = request.RequestedStart,
            RequestedEnd = request.RequestedEnd,
            AppointmentId = request.AppointmentId,
            AppointmentSnapshot = request.AppointmentSnapshot,
            CreatedAt = now,
            UpdatedAt = now,
            CreatedBy = customerId
        };

        if (request.Tasks.Count == 0)
        {
            project.Tasks.Add(new ProjectTask
            {
                TaskId = Guid.NewGuid(),
                ProjectId = project.ProjectId,
                Title = project.Title,
                ServiceType = "General Service",
                Detail = project.Description,
                CreatedAt = now,
                UpdatedAt = now
            });
        }
        else
        {
            foreach (var task in request.Tasks)
            {
                project.Tasks.Add(new ProjectTask
                {
                    TaskId = Guid.NewGuid(),
                    ProjectId = project.ProjectId,
                    Title = task.Title.Trim(),
                    ServiceType = task.ServiceType.Trim(),
                    Detail = task.Detail?.Trim(),
                    ScheduledStart = task.ScheduledStart,
                    ScheduledEnd = task.ScheduledEnd,
                    CreatedAt = now,
                    UpdatedAt = now
                });
            }
        }

        project.Activity.Add(new ProjectActivity
        {
            ActorId = customerId,
            ActorRole = "customer",
            Message = "Project requested by customer",
            CreatedAt = now
        });

        await _db.Projects.AddAsync(project, cancellationToken);
        await _db.SaveChangesAsync(cancellationToken);

        var saved = await LoadProjectAsync(project.ProjectId, cancellationToken);
        return CreatedAtAction(nameof(GetProjectById), new { projectId = project.ProjectId }, saved!.ToDetails());
    }

    [HttpGet("mine")]
    [Authorize(Policy = "CustomerOnly")]
    public async Task<ActionResult<IEnumerable<ProjectSummaryDto>>> GetMyProjects(CancellationToken cancellationToken)
    {
        var customerId = User.GetUserId();
        var projects = await _db.Projects
            .AsNoTracking()
            .Where(p => p.CustomerId == customerId)
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync(cancellationToken);

        return Ok(projects.Select(p => p.ToSummary()));
    }

    [HttpGet("{projectId:guid}")]
    [Authorize]
    public async Task<ActionResult<ProjectDetailsDto>> GetProjectById(Guid projectId, CancellationToken cancellationToken)
    {
        var project = await LoadProjectAsync(projectId, cancellationToken);
        if (project is null)
        {
            return NotFound();
        }

        if (!CanAccessProject(project))
        {
            return Forbid();
        }

        return Ok(project.ToDetails());
    }

    [HttpPost("{projectId:guid}/cancel")]
    [Authorize(Policy = "CustomerOnly")]
    public async Task<IActionResult> CancelProject(Guid projectId, CancellationToken cancellationToken)
    {
        var customerId = User.GetUserId();
        var project = await _db.Projects.FirstOrDefaultAsync(p => p.ProjectId == projectId && p.CustomerId == customerId, cancellationToken);
        if (project is null)
        {
            return NotFound();
        }

        if (project.Status != ProjectStatus.PendingReview)
        {
            return BadRequest("Project can only be cancelled while pending review.");
        }

        project.Status = ProjectStatus.Cancelled;
        project.UpdatedAt = DateTimeOffset.UtcNow;
        project.Activity.Add(new ProjectActivity
        {
            ActorId = customerId,
            ActorRole = "customer",
            Message = "Project cancelled by customer",
            CreatedAt = DateTimeOffset.UtcNow
        });

        await _db.SaveChangesAsync(cancellationToken);
        return NoContent();
    }

    private bool CanAccessProject(Project project)
    {
        var role = User.GetPrimaryRole()?.ToUpperInvariant();
        var userId = User.GetUserId();

        if (role == "ADMIN")
        {
            return true;
        }

        if (role == "CUSTOMER" && project.CustomerId == userId)
        {
            return true;
        }

        if (role == "EMPLOYEE")
        {
            return project.Tasks.Any(t => t.AssigneeId == userId);
        }

        return false;
    }

    private Task<Project?> LoadProjectAsync(Guid projectId, CancellationToken cancellationToken)
    {
        return _db.Projects
            .Include(p => p.Tasks)
            .Include(p => p.Activity)
            .FirstOrDefaultAsync(p => p.ProjectId == projectId, cancellationToken);
    }
}
