using System;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Extensions;
using Xunit;

namespace ProjectService.Tests;

public class ProjectMappingTests
{
    [Fact]
    public void ToSummary_MapsCoreFields()
    {
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 42,
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            Status = ProjectStatus.PendingReview,
            CreatedAt = DateTimeOffset.UtcNow.AddDays(-1),
            UpdatedAt = DateTimeOffset.UtcNow
        };

        var summary = project.ToSummary();

        Assert.Equal(project.ProjectId, summary.ProjectId);
        Assert.Equal(project.VehicleId, summary.VehicleId);
        Assert.Equal(project.Title, summary.Title);
        Assert.Equal(project.Status, summary.Status);
    }
}
