using System;
using System.Linq;
using FluentAssertions;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Extensions;
using Xunit;
using TaskStatus = ProjectService.Domain.Enums.TaskStatus;

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

    [Fact]
    public void ToSummary_MapsDateFields()
    {
        // Arrange
        var now = DateTimeOffset.UtcNow;
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            CreatedAt = now.AddDays(-5),
            UpdatedAt = now,
            RequestedStart = now.AddDays(1),
            RequestedEnd = now.AddDays(3),
            ApprovedStart = now.AddDays(2),
            ApprovedEnd = now.AddDays(4)
        };

        // Act
        var summary = project.ToSummary();

        // Assert
        summary.CreatedAt.Should().Be(project.CreatedAt);
        summary.UpdatedAt.Should().Be(project.UpdatedAt);
        summary.RequestedStart.Should().Be(project.RequestedStart);
        summary.RequestedEnd.Should().Be(project.RequestedEnd);
        summary.ApprovedStart.Should().Be(project.ApprovedStart);
        summary.ApprovedEnd.Should().Be(project.ApprovedEnd);
    }

    [Fact]
    public void ToSummary_WithNullOptionalFields_ReturnsValidDto()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            RequestedStart = null,
            RequestedEnd = null,
            ApprovedStart = null,
            ApprovedEnd = null
        };

        // Act
        var summary = project.ToSummary();

        // Assert
        summary.Should().NotBeNull();
        summary.RequestedStart.Should().BeNull();
        summary.RequestedEnd.Should().BeNull();
        summary.ApprovedStart.Should().BeNull();
        summary.ApprovedEnd.Should().BeNull();
    }

    [Fact]
    public void ToDetails_MapsAllProjectFields()
    {
        // Arrange
        var projectId = Guid.NewGuid();
        var appointmentId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;
        var project = new Project
        {
            ProjectId = projectId,
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Complete Service",
            Description = "Full vehicle inspection and service",
            Status = ProjectStatus.InProgress,
            CreatedAt = now,
            UpdatedAt = now,
            RequestedStart = now.AddDays(1),
            RequestedEnd = now.AddDays(2),
            ApprovedStart = now.AddDays(1),
            ApprovedEnd = now.AddDays(2),
            AppointmentId = appointmentId,
            AppointmentSnapshot = "{\"test\":\"data\"}"
        };

        // Act
        var details = project.ToDetails();

        // Assert
        details.Should().NotBeNull();
        details.ProjectId.Should().Be(projectId);
        details.CustomerId.Should().Be(123);
        details.VehicleId.Should().Be(project.VehicleId);
        details.Title.Should().Be("Complete Service");
        details.Description.Should().Be("Full vehicle inspection and service");
        details.Status.Should().Be(ProjectStatus.InProgress);
        details.AppointmentId.Should().Be(appointmentId);
        details.AppointmentSnapshot.Should().Be("{\"test\":\"data\"}");
    }

    [Fact]
    public void ToDetails_WithTasksAndActivity_MapsCollections()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            VehicleId = Guid.NewGuid(),
            Title = "Test Project"
        };

        var task1 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Task 1",
            ServiceType = "Repair",
            CreatedAt = DateTimeOffset.UtcNow.AddHours(-2)
        };

        var task2 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Task 2",
            ServiceType = "Maintenance",
            CreatedAt = DateTimeOffset.UtcNow.AddHours(-1)
        };

        var activity1 = new ProjectActivity
        {
            Id = 1,
            ProjectId = project.ProjectId,
            Message = "First activity",
            CreatedAt = DateTimeOffset.UtcNow.AddMinutes(-30)
        };

        var activity2 = new ProjectActivity
        {
            Id = 2,
            ProjectId = project.ProjectId,
            Message = "Second activity",
            CreatedAt = DateTimeOffset.UtcNow
        };

        project.Tasks.Add(task1);
        project.Tasks.Add(task2);
        project.Activity.Add(activity1);
        project.Activity.Add(activity2);

        // Act
        var details = project.ToDetails();

        // Assert
        details.Tasks.Should().HaveCount(2);
        var tasksList = details.Tasks.ToList();
        tasksList[0].Title.Should().Be("Task 1"); // Ordered by CreatedAt
        tasksList[1].Title.Should().Be("Task 2");
        
        details.Activity.Should().HaveCount(2);
        var activityList = details.Activity.ToList();
        activityList[0].Message.Should().Be("Second activity"); // Ordered by CreatedAt descending
        activityList[1].Message.Should().Be("First activity");
    }

    [Fact]
    public void ToDetails_WithEmptyCollections_ReturnsEmptyArrays()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            VehicleId = Guid.NewGuid(),
            Title = "Test"
        };

        // Act
        var details = project.ToDetails();

        // Assert
        details.Tasks.Should().NotBeNull().And.BeEmpty();
        details.Activity.Should().NotBeNull().And.BeEmpty();
    }

    [Fact]
    public void TaskToDto_MapsAllFields()
    {
        // Arrange
        var taskId = Guid.NewGuid();
        var appointmentId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;
        var task = new ProjectTask
        {
            TaskId = taskId,
            Title = "Oil Change",
            ServiceType = "Maintenance",
            Detail = "Replace oil and filter",
            Status = TaskStatus.InProgress,
            AssigneeId = 456,
            EstimateHours = 2.5m,
            ScheduledStart = now.AddDays(1),
            ScheduledEnd = now.AddDays(1).AddHours(2.5),
            AppointmentId = appointmentId,
            CreatedAt = now.AddDays(-1),
            UpdatedAt = now
        };

        // Act
        var dto = task.ToDto();

        // Assert
        dto.TaskId.Should().Be(taskId);
        dto.Title.Should().Be("Oil Change");
        dto.ServiceType.Should().Be("Maintenance");
        dto.Detail.Should().Be("Replace oil and filter");
        dto.Status.Should().Be(TaskStatus.InProgress);
        dto.AssigneeId.Should().Be(456);
        dto.EstimateHours.Should().Be(2.5m);
        dto.ScheduledStart.Should().Be(task.ScheduledStart);
        dto.ScheduledEnd.Should().Be(task.ScheduledEnd);
        dto.AppointmentId.Should().Be(appointmentId);
        dto.CreatedAt.Should().Be(task.CreatedAt);
        dto.UpdatedAt.Should().Be(task.UpdatedAt);
    }

    [Fact]
    public void TaskToDto_WithNullOptionalFields_ReturnsValidDto()
    {
        // Arrange
        var task = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            Title = "Test Task",
            ServiceType = "Test",
            Detail = null,
            AssigneeId = null,
            EstimateHours = null,
            ScheduledStart = null,
            ScheduledEnd = null,
            AppointmentId = null
        };

        // Act
        var dto = task.ToDto();

        // Assert
        dto.Should().NotBeNull();
        dto.Detail.Should().BeNull();
        dto.AssigneeId.Should().BeNull();
        dto.EstimateHours.Should().BeNull();
        dto.ScheduledStart.Should().BeNull();
        dto.ScheduledEnd.Should().BeNull();
        dto.AppointmentId.Should().BeNull();
    }

    [Fact]
    public void ActivityToDto_MapsAllFields()
    {
        // Arrange
        var taskId = Guid.NewGuid();
        var projectId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;
        var activity = new ProjectActivity
        {
            Id = 42,
            ProjectId = projectId,
            TaskId = taskId,
            ActorId = 123,
            ActorRole = "admin",
            Message = "Task completed",
            CreatedAt = now
        };

        // Act
        var dto = activity.ToDto();

        // Assert
        dto.Id.Should().Be(42);
        dto.TaskId.Should().Be(taskId);
        dto.ActorId.Should().Be(123);
        dto.ActorRole.Should().Be("admin");
        dto.Message.Should().Be("Task completed");
        dto.CreatedAt.Should().Be(now);
    }

    [Fact]
    public void ActivityToDto_WithNullTaskId_ReturnsValidDto()
    {
        // Arrange
        var activity = new ProjectActivity
        {
            Id = 1,
            ProjectId = Guid.NewGuid(),
            TaskId = null,
            ActorId = 100,
            ActorRole = "system",
            Message = "Project created"
        };

        // Act
        var dto = activity.ToDto();

        // Assert
        dto.Should().NotBeNull();
        dto.TaskId.Should().BeNull();
        dto.ActorId.Should().Be(100);
    }
}
