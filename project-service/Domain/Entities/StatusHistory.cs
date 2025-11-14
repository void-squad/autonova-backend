using System.ComponentModel.DataAnnotations;
using ProjectService.Domain.Enums;

namespace ProjectService.Domain.Entities;

public class StatusHistory
{
    public long Id { get; set; }
    public Guid ProjectId { get; set; }
    public ProjectStatus FromStatus { get; set; }
    public ProjectStatus ToStatus { get; set; }
    public Guid ChangedBy { get; set; }
    public DateTimeOffset ChangedAt { get; set; }
    [MaxLength(240)]
    public string? Note { get; set; }

    public Project? Project { get; set; }
}
