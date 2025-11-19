using FluentAssertions;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using System;
using Xunit;
using TaskStatus = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Tests;

public class ProjectTaskEntityTests
{
    [Fact]
    public void ProjectTask_DefaultValues_AreSet()
    {
        // Arrange & Act
        var task = new ProjectTask();

        // Assert
        task.Title.Should().BeEmpty();
        task.ServiceType.Should().BeEmpty();
        task.Status.Should().Be(TaskStatus.Requested);
        task.CreatedAt.Should().BeCloseTo(DateTimeOffset.UtcNow, TimeSpan.FromSeconds(1));
        task.UpdatedAt.Should().BeCloseTo(DateTimeOffset.UtcNow, TimeSpan.FromSeconds(1));
        task.Activity.Should().NotBeNull().And.BeEmpty();
    }

    [Fact]
    public void ProjectTask_CanSetAllProperties()
    {
        // Arrange
        var taskId = Guid.NewGuid();
        var projectId = Guid.NewGuid();
        var appointmentId = Guid.NewGuid();
        var assigneeId = 789L;
        var now = DateTimeOffset.UtcNow;

        // Act
        var task = new ProjectTask
        {
            TaskId = taskId,
            ProjectId = projectId,
            Title = "Oil Change",
            ServiceType = "Maintenance",
            Detail = "Replace engine oil and filter",
            Status = TaskStatus.InProgress,
            AssigneeId = assigneeId,
            EstimateHours = 2.5m,
            ScheduledStart = now.AddDays(1),
            ScheduledEnd = now.AddDays(1).AddHours(2.5),
            AppointmentId = appointmentId,
            CreatedAt = now,
            UpdatedAt = now
        };

        // Assert
        task.TaskId.Should().Be(taskId);
        task.ProjectId.Should().Be(projectId);
        task.Title.Should().Be("Oil Change");
        task.ServiceType.Should().Be("Maintenance");
        task.Detail.Should().Be("Replace engine oil and filter");
        task.Status.Should().Be(TaskStatus.InProgress);
        task.AssigneeId.Should().Be(assigneeId);
        task.EstimateHours.Should().Be(2.5m);
        task.AppointmentId.Should().Be(appointmentId);
    }

    [Theory]
    [InlineData(TaskStatus.Pending)]
    [InlineData(TaskStatus.Requested)]
    [InlineData(TaskStatus.Accepted)]
    [InlineData(TaskStatus.InProgress)]
    [InlineData(TaskStatus.Completed)]
    [InlineData(TaskStatus.Cancelled)]
    public void ProjectTask_CanBeInAnyStatus(TaskStatus status)
    {
        // Arrange & Act
        var task = new ProjectTask { Status = status };

        // Assert
        task.Status.Should().Be(status);
    }

    [Fact]
    public void ProjectTask_CanBeLinkedToProject()
    {
        // Arrange
        var project = new Project { ProjectId = Guid.NewGuid() };
        var task = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Project = project
        };

        // Act & Assert
        task.Project.Should().Be(project);
        task.ProjectId.Should().Be(project.ProjectId);
    }

    [Fact]
    public void ProjectTask_CanAddActivity()
    {
        // Arrange
        var task = new ProjectTask { TaskId = Guid.NewGuid() };
        var activity = new ProjectActivity
        {
            Id = 1L,
            TaskId = task.TaskId,
            Message = "Task started"
        };

        // Act
        task.Activity.Add(activity);

        // Assert
        task.Activity.Should().HaveCount(1);
        task.Activity.Should().Contain(activity);
    }

    [Fact]
    public void ProjectTask_WithoutAssignee_HasNullAssigneeId()
    {
        // Arrange & Act
        var task = new ProjectTask
        {
            Title = "Unassigned Task",
            AssigneeId = null
        };

        // Assert
        task.AssigneeId.Should().BeNull();
    }

    [Fact]
    public void ProjectTask_WithoutAppointment_HasNullAppointmentId()
    {
        // Arrange & Act
        var task = new ProjectTask
        {
            Title = "Task without appointment",
            AppointmentId = null
        };

        // Assert
        task.AppointmentId.Should().BeNull();
    }

    [Fact]
    public void ProjectTask_WithoutEstimate_HasNullEstimateHours()
    {
        // Arrange & Act
        var task = new ProjectTask
        {
            Title = "Task without estimate",
            EstimateHours = null
        };

        // Assert
        task.EstimateHours.Should().BeNull();
    }

    [Fact]
    public void ProjectTask_WithoutSchedule_HasNullScheduledDates()
    {
        // Arrange & Act
        var task = new ProjectTask
        {
            Title = "Unscheduled task",
            ScheduledStart = null,
            ScheduledEnd = null
        };

        // Assert
        task.ScheduledStart.Should().BeNull();
        task.ScheduledEnd.Should().BeNull();
    }
}
