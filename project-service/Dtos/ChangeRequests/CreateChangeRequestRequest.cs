namespace ProjectService.Dtos.ChangeRequests;

public class CreateChangeRequestRequest
{
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public decimal? ProposedPriceDelta { get; set; }
    public int? ProposedExtraHours { get; set; }
    public DateOnly? ProposedNewDueDate { get; set; }
}
