using ProjectService.Domain.Enums;

namespace ProjectService.Dtos;

public class ProjectResponse
{
    public long Id { get; set; }
    public Guid ProjectId { get; set; }
    public Guid CustomerId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public ProjectStatus Status { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public decimal Budget { get; set; }
    public DateOnly? DueDate { get; set; }
    public string? ClientRequestId { get; set; }
    public IReadOnlyCollection<TaskResponse> Tasks { get; set; } = Array.Empty<TaskResponse>();
    public IReadOnlyCollection<QuoteResponse> Quotes { get; set; } = Array.Empty<QuoteResponse>();
    public IReadOnlyCollection<StatusHistoryResponse> StatusHistory { get; set; } = Array.Empty<StatusHistoryResponse>();

    public class TaskResponse
    {
        public Guid TaskId { get; set; }
        public string Title { get; set; } = string.Empty;
        public decimal EstimateHours { get; set; }
        public Guid? AssigneeId { get; set; }
        public string Status { get; set; } = string.Empty;
    }

    public class QuoteResponse
    {
        public Guid QuoteId { get; set; }
        public decimal Total { get; set; }
        public Domain.Enums.QuoteStatus Status { get; set; }
        public DateTimeOffset IssuedAt { get; set; }
        public DateTimeOffset? ApprovedAt { get; set; }
        public DateTimeOffset? RejectedAt { get; set; }
        public Guid? ApprovedBy { get; set; }
        public Guid? RejectedBy { get; set; }
        public string? ClientRequestId { get; set; }
        public uint xmin { get; set; }
    }

    public class StatusHistoryResponse
    {
        public long Id { get; set; }
        public ProjectStatus FromStatus { get; set; }
        public ProjectStatus ToStatus { get; set; }
        public Guid ChangedBy { get; set; }
        public DateTimeOffset ChangedAt { get; set; }
        public string? Note { get; set; }
    }
}
