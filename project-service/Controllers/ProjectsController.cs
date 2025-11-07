using System.Net.Mime;
using FluentValidation;
using FluentValidation.Results;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
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
    private readonly IProjectWorkflowService _workflowService;
    private readonly IValidator<CreateProjectRequest> _createValidator;
    private readonly IValidator<UpdateProjectStatusRequest> _statusValidator;
    private readonly ILogger<ProjectsController> _logger;

    public ProjectsController(
        IProjectWorkflowService workflowService,
        IValidator<CreateProjectRequest> createValidator,
        IValidator<UpdateProjectStatusRequest> statusValidator,
        ILogger<ProjectsController> logger)
    {
        _workflowService = workflowService;
        _createValidator = createValidator;
        _statusValidator = statusValidator;
        _logger = logger;
    }

    [HttpPost]
    [Authorize(Policy = "Customer")]
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
    [Authorize]
    [ProducesResponseType(typeof(IEnumerable<ProjectResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetProjects([FromQuery] ProjectStatus? status, [FromQuery] Guid? customerId, CancellationToken cancellationToken)
    {
        var projects = await _workflowService.GetProjectsAsync(status, customerId, cancellationToken);
        return Ok(projects.Select(p => p.ToResponse()));
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
    [Authorize(Policy = "EmployeeOrManager")]
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
