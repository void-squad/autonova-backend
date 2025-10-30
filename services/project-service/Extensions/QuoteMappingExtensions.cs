using ProjectService.Domain.Entities;
using ProjectService.Dtos;

namespace ProjectService.Extensions;

public static class QuoteMappingExtensions
{
    public static QuoteDetailResponse ToResponse(this Quote quote) => new()
    {
        QuoteId = quote.QuoteId,
        ProjectId = quote.ProjectId,
        Total = quote.Total,
        Status = quote.Status,
        IssuedAt = quote.IssuedAt,
        ApprovedAt = quote.ApprovedAt
    };
}
