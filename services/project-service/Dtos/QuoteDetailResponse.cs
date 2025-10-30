using ProjectService.Domain.Enums;

namespace ProjectService.Dtos;

public class QuoteDetailResponse
{
    public Guid QuoteId { get; set; }
    public Guid ProjectId { get; set; }
    public decimal Total { get; set; }
    public QuoteStatus Status { get; set; }
    public DateTimeOffset IssuedAt { get; set; }
    public DateTimeOffset? ApprovedAt { get; set; }
}
