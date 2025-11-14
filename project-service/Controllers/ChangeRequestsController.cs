using System.Net.Mime;
using FluentValidation;
using FluentValidation.Results;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ProjectService.Domain.Exceptions;
using ProjectService.Dtos.ChangeRequests;
using ProjectService.Extensions;
using ProjectService.Services;

namespace ProjectService.Controllers;

[ApiController]
[Route("api")]
[Produces(MediaTypeNames.Application.Json)]
public class ChangeRequestsController : ControllerBase
{
    private readonly IProjectWorkflowService _workflowService;
    private readonly IValidator<CreateChangeRequestRequest> _createValidator;
    private readonly ILogger<ChangeRequestsController> _logger;

    public ChangeRequestsController(
        IProjectWorkflowService workflowService,
        IValidator<CreateChangeRequestRequest> createValidator,
        ILogger<ChangeRequestsController> logger)
    {
        _workflowService = workflowService;
        _createValidator = createValidator;
        _logger = logger;
    }

    [HttpPost("projects/{projectId:guid}/change-requests")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(ChangeRequestResponse), StatusCodes.Status201Created)]
    public async Task<IActionResult> Create(Guid projectId, [FromBody] CreateChangeRequestRequest request, CancellationToken cancellationToken)
    {
        ValidationResult validation = await _createValidator.ValidateAsync(request, cancellationToken);
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
            var changeRequest = await _workflowService.CreateChangeRequestAsync(projectId, request, actorId, actorRole, clientRequestId, cancellationToken);
            return Created($"/api/change-requests/{changeRequest.ChangeRequestId}", changeRequest.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to create change request for project {ProjectId}", projectId);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpGet("projects/{projectId:guid}/change-requests")]
    [Authorize]
    [ProducesResponseType(typeof(IEnumerable<ChangeRequestResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> List(Guid projectId, CancellationToken cancellationToken)
    {
        var changeRequests = await _workflowService.GetProjectChangeRequestsAsync(projectId, cancellationToken);
        return Ok(changeRequests.Select(cr => cr.ToResponse()));
    }

    [HttpGet("change-requests/{changeRequestId:guid}")]
    [Authorize]
    [ProducesResponseType(typeof(ChangeRequestResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid changeRequestId, CancellationToken cancellationToken)
    {
        var changeRequest = await _workflowService.GetChangeRequestAsync(changeRequestId, cancellationToken);
        if (changeRequest is null)
        {
            return NotFound();
        }

        return Ok(changeRequest.ToResponse());
    }

    [HttpPost("change-requests/{changeRequestId:guid}/approve")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(ChangeRequestResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> Approve(Guid changeRequestId, [FromBody] ChangeRequestDecisionRequest request, CancellationToken cancellationToken)
    {
        if (request is null)
        {
            return BadRequest("Request body is required.");
        }

        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var changeRequest = await _workflowService.ApproveChangeRequestAsync(changeRequestId, request.RowVersion, actorId, actorRole, clientRequestId, cancellationToken);
            return Ok(changeRequest.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to approve change request {ChangeRequestId}", changeRequestId);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpPost("change-requests/{changeRequestId:guid}/reject")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(ChangeRequestResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> Reject(Guid changeRequestId, [FromBody] ChangeRequestDecisionRequest request, CancellationToken cancellationToken)
    {
        if (request is null)
        {
            return BadRequest("Request body is required.");
        }

        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var changeRequest = await _workflowService.RejectChangeRequestAsync(changeRequestId, request.RowVersion, actorId, actorRole, clientRequestId, cancellationToken);
            return Ok(changeRequest.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to reject change request {ChangeRequestId}", changeRequestId);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpPost("change-requests/{changeRequestId:guid}/apply")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(ChangeRequestResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> Apply(Guid changeRequestId, [FromBody] ChangeRequestDecisionRequest request, CancellationToken cancellationToken)
    {
        if (request is null)
        {
            return BadRequest("Request body is required.");
        }

        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var changeRequest = await _workflowService.ApplyChangeRequestAsync(changeRequestId, request.RowVersion, actorId, actorRole, clientRequestId, cancellationToken);
            return Ok(changeRequest.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to apply change request {ChangeRequestId}", changeRequestId);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }
}
