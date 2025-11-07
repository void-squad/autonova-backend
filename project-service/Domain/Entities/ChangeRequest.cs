using System.ComponentModel.DataAnnotations;
using ProjectService.Domain.Enums;

namespace ProjectService.Domain.Entities;

public class ChangeRequest
{
    public Guid ChangeRequestId { get; set; }
    public Guid ProjectId { get; set; }

    [MaxLength(120)]
    public string Title { get; set; } = default!;

    [MaxLength(4000)]
    public string? Description { get; set; }

    public decimal? ProposedPriceDelta { get; set; }
    public int? ProposedExtraHours { get; set; }
    public DateOnly? ProposedNewDueDate { get; set; }

    public ChangeRequestStatus Status { get; set; } = ChangeRequestStatus.Submitted;

    public Guid CreatedBy { get; set; }
    public DateTimeOffset CreatedAt { get; set; }

    public Guid? DecidedBy { get; set; }
    public DateTimeOffset? DecidedAt { get; set; }

    [MaxLength(64)]
    public string? ClientRequestId { get; set; }

    public uint xmin { get; set; }

    public Project Project { get; set; } = default!;
}
