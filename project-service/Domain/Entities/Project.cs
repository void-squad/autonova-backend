using System.ComponentModel.DataAnnotations;
using ProjectService.Domain.Enums;

namespace ProjectService.Domain.Entities;

public class Project
{
    public long Id { get; set; }
    public Guid ProjectId { get; set; }
    public Guid CustomerId { get; set; }
    public Guid VehicleId { get; set; }
    public string Title { get; set; } = string.Empty;
    public ProjectStatus Status { get; set; } = ProjectStatus.Requested;
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public decimal Budget { get; set; }
    public DateOnly? DueDate { get; set; }

    [MaxLength(64)]
    public string? ClientRequestId { get; set; }
    public uint xmin { get; set; }

    public ICollection<TaskItem> Tasks { get; set; } = new List<TaskItem>();
    public ICollection<Quote> Quotes { get; set; } = new List<Quote>();
    public ICollection<StatusHistory> StatusHistory { get; set; } = new List<StatusHistory>();
    public ICollection<ChangeRequest> ChangeRequests { get; set; } = new List<ChangeRequest>();
}
