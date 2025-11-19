using FluentAssertions;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using System;
using Xunit;

namespace ProjectService.Tests;

public class ProjectEntityTests
{
    [Fact]
    public void Project_DefaultValues_AreSet()
    {
        // Arrange & Act
        var project = new Project();

        // Assert
        project.Title.Should().BeEmpty();
        project.Status.Should().Be(ProjectStatus.PendingReview);
        project.CreatedAt.Should().BeCloseTo(DateTimeOffset.UtcNow, TimeSpan.FromSeconds(1));
        project.UpdatedAt.Should().BeCloseTo(DateTimeOffset.UtcNow, TimeSpan.FromSeconds(1));
        project.Tasks.Should().NotBeNull().And.BeEmpty();
        project.Activity.Should().NotBeNull().And.BeEmpty();
    }

    [Fact]
    public void Project_CanSetAllProperties()
    {
        // Arrange
        var projectId = Guid.NewGuid();
        var vehicleId = Guid.NewGuid();
        var appointmentId = Guid.NewGuid();
        var customerId = 123L;
        var createdBy = 456L;
        var now = DateTimeOffset.UtcNow;

        // Act
        var project = new Project
        {
            ProjectId = projectId,
            CustomerId = customerId,
            VehicleId = vehicleId,
            Title = "Engine Repair",
            Description = "Complete engine overhaul",
            Status = ProjectStatus.InProgress,
            CreatedAt = now,
            UpdatedAt = now,
            RequestedStart = now.AddDays(1),
            RequestedEnd = now.AddDays(3),
            ApprovedStart = now.AddDays(1),
            ApprovedEnd = now.AddDays(3),
            CreatedBy = createdBy,
            AppointmentId = appointmentId,
            AppointmentSnapshot = "{\"date\":\"2024-01-01\"}"
        };

        // Assert
        project.ProjectId.Should().Be(projectId);
        project.CustomerId.Should().Be(customerId);
        project.VehicleId.Should().Be(vehicleId);
        project.Title.Should().Be("Engine Repair");
        project.Description.Should().Be("Complete engine overhaul");
        project.Status.Should().Be(ProjectStatus.InProgress);
        project.CreatedBy.Should().Be(createdBy);
        project.AppointmentId.Should().Be(appointmentId);
    }

    [Fact]
    public void Project_CanAddTasks()
    {
        // Arrange
        var project = new Project { ProjectId = Guid.NewGuid() };
        var task1 = new ProjectTask { TaskId = Guid.NewGuid(), Title = "Task 1" };
        var task2 = new ProjectTask { TaskId = Guid.NewGuid(), Title = "Task 2" };

        // Act
        project.Tasks.Add(task1);
        project.Tasks.Add(task2);

        // Assert
        project.Tasks.Should().HaveCount(2);
        project.Tasks.Should().Contain(task1);
        project.Tasks.Should().Contain(task2);
    }

    [Fact]
    public void Project_CanAddActivity()
    {
        // Arrange
        var project = new Project { ProjectId = Guid.NewGuid() };
        var activity = new ProjectActivity
        {
            Id = 1L,
            ProjectId = project.ProjectId,
            Message = "Project created"
        };

        // Act
        project.Activity.Add(activity);

        // Assert
        project.Activity.Should().HaveCount(1);
        project.Activity.Should().Contain(activity);
    }

    [Theory]
    [InlineData(ProjectStatus.PendingReview)]
    [InlineData(ProjectStatus.Approved)]
    [InlineData(ProjectStatus.InProgress)]
    [InlineData(ProjectStatus.Completed)]
    [InlineData(ProjectStatus.Cancelled)]
    public void Project_CanBeInAnyStatus(ProjectStatus status)
    {
        // Arrange & Act
        var project = new Project { Status = status };

        // Assert
        project.Status.Should().Be(status);
    }

    [Fact]
    public void Project_WithNullDescription_IsAllowed()
    {
        // Arrange & Act
        var project = new Project
        {
            Title = "Test Project",
            Description = null
        };

        // Assert
        project.Description.Should().BeNull();
    }

    [Fact]
    public void Project_WithOptionalDates_AllowsNull()
    {
        // Arrange & Act
        var project = new Project
        {
            RequestedStart = null,
            RequestedEnd = null,
            ApprovedStart = null,
            ApprovedEnd = null
        };

        // Assert
        project.RequestedStart.Should().BeNull();
        project.RequestedEnd.Should().BeNull();
        project.ApprovedStart.Should().BeNull();
        project.ApprovedEnd.Should().BeNull();
    }
}
