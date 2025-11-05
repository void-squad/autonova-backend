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

    public Project? Project { get; set; }
}
