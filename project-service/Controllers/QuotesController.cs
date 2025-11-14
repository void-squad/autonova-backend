using System.Net.Mime;
using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ProjectService.Domain.Exceptions;
using ProjectService.Dtos;
using ProjectService.Extensions;
using ProjectService.Services;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/quotes")]
[Produces(MediaTypeNames.Application.Json)]
public class QuotesController : ControllerBase
{
    private readonly IProjectWorkflowService _workflowService;
    private readonly IValidator<CreateQuoteRequest> _createValidator;
    private readonly ILogger<QuotesController> _logger;

    public QuotesController(
        IProjectWorkflowService workflowService,
        IValidator<CreateQuoteRequest> createValidator,
        ILogger<QuotesController> logger)
    {
        _workflowService = workflowService;
        _createValidator = createValidator;
        _logger = logger;
    }

    [HttpPost("~/api/projects/{projectId:guid}/quotes")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(QuoteDetailResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> CreateQuote(Guid projectId, [FromBody] CreateQuoteRequest request, CancellationToken cancellationToken)
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
            var quote = await _workflowService.CreateQuoteAsync(projectId, request, actorId, actorRole, clientRequestId, cancellationToken);
            return Created($"/api/quotes/{quote.QuoteId}", quote.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to create quote for project {ProjectId}", projectId);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpPost("{id:guid}/approve")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(QuoteDetailResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ApproveQuote(Guid id, CancellationToken cancellationToken)
    {
        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var quote = await _workflowService.ApproveQuoteAsync(id, actorId, actorRole, clientRequestId, cancellationToken);
            return Ok(quote.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to approve quote {QuoteId}", id);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }

    [HttpPost("{id:guid}/reject")]
    [Authorize(Policy = "AdminOnly")]
    [ProducesResponseType(typeof(QuoteDetailResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> RejectQuote(Guid id, CancellationToken cancellationToken)
    {
        try
        {
            var actorId = User.GetUserId();
            var actorRole = User.GetPrimaryRole();
            var clientRequestId = Request.GetIdempotencyKey();
            var quote = await _workflowService.RejectQuoteAsync(id, actorId, actorRole, clientRequestId, cancellationToken);
            return Ok(quote.ToResponse());
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Failed to reject quote {QuoteId}", id);
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
    }
}
