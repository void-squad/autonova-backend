using FluentAssertions;
using ProjectService.Domain.Entities;
using System;
using Xunit;

namespace ProjectService.Tests;

public class ProjectActivityEntityTests
{
    [Fact]
    public void ProjectActivity_DefaultValues_AreSet()
    {
        // Arrange & Act
        var activity = new ProjectActivity();

        // Assert
        activity.ActorRole.Should().BeEmpty();
        activity.Message.Should().BeEmpty();
        activity.CreatedAt.Should().BeCloseTo(DateTimeOffset.UtcNow, TimeSpan.FromSeconds(1));
    }

    [Fact]
    public void ProjectActivity_CanSetAllProperties()
    {
        // Arrange
        var projectId = Guid.NewGuid();
        var taskId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;

        // Act
        var activity = new ProjectActivity
        {
            Id = 42,
            ProjectId = projectId,
            TaskId = taskId,
            ActorId = 123,
            ActorRole = "admin",
            Message = "Project updated",
            CreatedAt = now
        };

        // Assert
        activity.Id.Should().Be(42);
        activity.ProjectId.Should().Be(projectId);
        activity.TaskId.Should().Be(taskId);
        activity.ActorId.Should().Be(123);
        activity.ActorRole.Should().Be("admin");
        activity.Message.Should().Be("Project updated");
        activity.CreatedAt.Should().Be(now);
    }

    [Fact]
    public void ProjectActivity_WithoutTaskId_HasNullTaskId()
    {
        // Arrange & Act
        var activity = new ProjectActivity
        {
            ProjectId = Guid.NewGuid(),
            TaskId = null,
            ActorId = 100,
            Message = "General activity"
        };

        // Assert
        activity.TaskId.Should().BeNull();
    }

    [Fact]
    public void ProjectActivity_CanBeLinkedToProject()
    {
        // Arrange
        var project = new Project { ProjectId = Guid.NewGuid() };
        var activity = new ProjectActivity
        {
            ProjectId = project.ProjectId,
            Project = project,
            Message = "Activity"
        };

        // Act & Assert
        activity.Project.Should().Be(project);
        activity.ProjectId.Should().Be(project.ProjectId);
    }

    [Fact]
    public void ProjectActivity_CanBeLinkedToTask()
    {
        // Arrange
        var task = new ProjectTask { TaskId = Guid.NewGuid() };
        var activity = new ProjectActivity
        {
            TaskId = task.TaskId,
            Task = task,
            Message = "Task activity"
        };

        // Act & Assert
        activity.Task.Should().Be(task);
        activity.TaskId.Should().Be(task.TaskId);
    }

    [Fact]
    public void ProjectActivity_SupportsLongActorIds()
    {
        // Arrange & Act
        var activity = new ProjectActivity
        {
            ActorId = long.MaxValue,
            Message = "Test"
        };

        // Assert
        activity.ActorId.Should().Be(long.MaxValue);
    }

    [Fact]
    public void ProjectActivity_SupportsEmptyMessage()
    {
        // Arrange & Act
        var activity = new ProjectActivity
        {
            Message = string.Empty
        };

        // Assert
        activity.Message.Should().BeEmpty();
    }

    [Fact]
    public void ProjectActivity_SupportsLongMessage()
    {
        // Arrange
        var longMessage = new string('a', 1000);

        // Act
        var activity = new ProjectActivity
        {
            Message = longMessage
        };

        // Assert
        activity.Message.Should().Be(longMessage);
        activity.Message.Length.Should().Be(1000);
    }

    [Theory]
    [InlineData("admin")]
    [InlineData("employee")]
    [InlineData("customer")]
    [InlineData("manager")]
    [InlineData("system")]
    public void ProjectActivity_SupportsVariousRoles(string role)
    {
        // Arrange & Act
        var activity = new ProjectActivity
        {
            ActorRole = role,
            Message = "Test"
        };

        // Assert
        activity.ActorRole.Should().Be(role);
    }
}
