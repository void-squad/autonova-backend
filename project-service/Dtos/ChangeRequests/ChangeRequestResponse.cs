using ProjectService.Domain.Enums;

namespace ProjectService.Dtos.ChangeRequests;

public class ChangeRequestResponse
{
    public Guid ChangeRequestId { get; set; }
    public Guid ProjectId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public decimal? ProposedPriceDelta { get; set; }
    public int? ProposedExtraHours { get; set; }
    public DateOnly? ProposedNewDueDate { get; set; }
    public ChangeRequestStatus Status { get; set; }
    public Guid CreatedBy { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public Guid? DecidedBy { get; set; }
    public DateTimeOffset? DecidedAt { get; set; }
    public string? ClientRequestId { get; set; }
    public uint xmin { get; set; }
}
