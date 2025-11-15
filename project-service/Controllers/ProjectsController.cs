using System.Net.Mime;
using FluentValidation;
using FluentValidation.Results;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Domain.Exceptions;
using ProjectService.Dtos;
using ProjectService.Extensions;
using ProjectService.Services;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/[controller]")]
[Produces(MediaTypeNames.Application.Json)]
public class ProjectsController : ControllerBase
{
    private readonly AppDb _db;
    private readonly IProjectWorkflowService _workflowService;
    private readonly IValidator<CreateProjectRequest> _createValidator;
    private readonly IValidator<UpdateProjectStatusRequest> _statusValidator;
    private readonly ILogger<ProjectsController> _logger;

    public ProjectsController(
        AppDb db,
        IProjectWorkflowService workflowService,
        IValidator<CreateProjectRequest> createValidator,
        IValidator<UpdateProjectStatusRequest> statusValidator,
        ILogger<ProjectsController> logger)
    {
        _db = db;
        _workflowService = workflowService;
        _createValidator = createValidator;
        _statusValidator = statusValidator;
        _logger = logger;
    }

    [HttpGet("employee/{assigneeId:long}")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(IEnumerable<EmployeeProjectResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<EmployeeProjectResponse>>> GetProjectsForEmployee(long assigneeId, CancellationToken cancellationToken)
    {
        var projects = await _db.Projects
            .AsNoTracking()
            .Where(p => p.Tasks.Any(t => t.AssigneeId == assigneeId))
            .OrderBy(p => p.DueDate == null)
            .ThenBy(p => p.DueDate)
            .ThenByDescending(p => p.CreatedAt)
            .Select(p => new EmployeeProjectResponse
            {
                Id = p.Id,
                ProjectId = p.ProjectId,
                VehicleId = p.VehicleId,
                Title = p.Title,
                Status = p.Status,
                DueDate = p.DueDate
            })
            .ToListAsync(cancellationToken);

        return Ok(projects);
    }

    [HttpPost]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(ProjectResponse), StatusCodes.Status201Created)]
    public async Task<IActionResult> CreateProject([FromBody] CreateProjectRequest request, CancellationToken cancellationToken)
    {
        var validation = await _createValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            var details = new ValidationProblemDetails(validation.ToProblemDictionary())
            {
                Status = StatusCodes.Status400BadRequest
            };
            return ValidationProblem(details);
        }

        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var project = await _workflowService.CreateProjectAsync(request, actorId, actorRole, clientRequestId, cancellationToken);
            var full = await _workflowService.GetProjectAsync(project.ProjectId, cancellationToken) ?? project;
            var response = full.ToResponse();
            return CreatedAtAction(nameof(GetProjectById), new { id = response.ProjectId }, response);
        }
        catch (DomainException ex)
        {
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpGet]
    [AllowAnonymous]
    [ProducesResponseType(typeof(IEnumerable<ProjectResponse>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProjectListResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetProjects(
        [FromQuery] ProjectStatus? status,
        [FromQuery] long? customerId,
        [FromQuery] long? assigneeId,
        [FromQuery] bool includeTasks = false,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20,
        CancellationToken cancellationToken = default)
    {
        if (!assigneeId.HasValue)
        {
            if (includeTasks)
            {
                return BadRequest("includeTasks requires an assigneeId.");
            }

            var projects = await _workflowService.GetProjectsAsync(status, customerId, cancellationToken);
            return Ok(projects.Select(p => p.ToResponse()));
        }

        page = Math.Max(1, page);
        pageSize = Math.Clamp(pageSize, 1, 200);

        var query = _db.Projects
            .Include(x => x.Tasks)
            .Include(x => x.Quotes)
            .Include(x => x.StatusHistory)
            .AsNoTracking()
            .AsQueryable();

        if (status.HasValue)
        {
            query = query.Where(x => x.Status == status.Value);
        }

        if (customerId.HasValue)
        {
            query = query.Where(x => x.CustomerId == customerId.Value);
        }

        var employeeId = assigneeId.Value;
        query = query.Where(p => p.Tasks.Any(t => t.AssigneeId == employeeId));

        var total = await query.CountAsync(cancellationToken);

        var projectsPage = await query
            .OrderByDescending(x => x.CreatedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(cancellationToken);

        foreach (var project in projectsPage)
        {
            if (includeTasks)
            {
                project.Tasks = project.Tasks.Where(t => t.AssigneeId == employeeId).ToList();
            }
            else
            {
                project.Tasks = new List<TaskItem>();
            }
        }

        var response = new ProjectListResponse
        {
            Page = page,
            PageSize = pageSize,
            Total = total,
            Items = projectsPage.Select(p => p.ToResponse()).ToList()
        };

        return Ok(response);
    }

    [HttpGet("{id:guid}")]
    [Authorize]
    [ProducesResponseType(typeof(ProjectResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetProjectById(Guid id, CancellationToken cancellationToken)
    {
        var project = await _workflowService.GetProjectAsync(id, cancellationToken);
        if (project is null)
        {
            return NotFound();
        }

        return Ok(project.ToResponse());
    }

    [HttpPatch("{id:guid}/status")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(ProjectResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateStatus(Guid id, [FromBody] UpdateProjectStatusRequest request, CancellationToken cancellationToken)
    {
        ValidationResult validation = await _statusValidator.ValidateAsync(request, cancellationToken);
        if (!validation.IsValid)
        {
            var details = new ValidationProblemDetails(validation.ToProblemDictionary())
            {
                Status = StatusCodes.Status400BadRequest
            };
            return ValidationProblem(details);
        }

        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var project = await _workflowService.UpdateStatusAsync(id, request.NewStatus, actorId, actorRole, clientRequestId, cancellationToken);
            var hydrated = await _workflowService.GetProjectAsync(project.ProjectId, cancellationToken) ?? project;
            return Ok(hydrated.ToResponse());
        }
        catch (DomainException ex)
        {
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpGet("{id:guid}/status-history")]
    [Authorize]
    [ProducesResponseType(typeof(IEnumerable<ProjectResponse.StatusHistoryResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetStatusHistory(Guid id, CancellationToken cancellationToken)
    {
        var history = await _workflowService.GetStatusHistoryAsync(id, cancellationToken);
        var response = history.Select(h => new ProjectResponse.StatusHistoryResponse
        {
            Id = h.Id,
            FromStatus = h.FromStatus,
            ToStatus = h.ToStatus,
            ChangedBy = h.ChangedBy,
            ChangedAt = h.ChangedAt,
            Note = h.Note
        });

        return Ok(response);
    }
}
