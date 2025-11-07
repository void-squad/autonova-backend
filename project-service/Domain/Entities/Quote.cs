using System.ComponentModel.DataAnnotations;
using ProjectService.Domain.Enums;

namespace ProjectService.Domain.Entities;

public class Quote
{
    public Guid QuoteId { get; set; }
    public Guid ProjectId { get; set; }
    public decimal Total { get; set; }
    public QuoteStatus Status { get; set; } = QuoteStatus.Draft;
    public DateTimeOffset IssuedAt { get; set; }
    public DateTimeOffset? ApprovedAt { get; set; }
    public DateTimeOffset? RejectedAt { get; set; }
    public Guid? ApprovedBy { get; set; }
    public Guid? RejectedBy { get; set; }

    [MaxLength(64)]
    public string? ClientRequestId { get; set; }
    public uint xmin { get; set; }

    public Project? Project { get; set; }
}
