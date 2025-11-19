using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using System;
using System.Linq;
using System.Threading.Tasks;
using Xunit;
using TaskStatus = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Tests;

public class AppDbContextTests : IDisposable
{
    private readonly AppDb _context;

    public AppDbContextTests()
    {
        var options = new DbContextOptionsBuilder<AppDb>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new AppDb(options);
    }

    [Fact]
    public async Task AppDb_CanSaveAndRetrieveProject()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Test Project",
            Description = "Test Description",
            Status = ProjectStatus.Approved,
            CreatedBy = 456
        };

        // Act
        _context.Projects.Add(project);
        await _context.SaveChangesAsync();

        // Assert
        var retrieved = await _context.Projects.FindAsync(project.ProjectId);
        retrieved.Should().NotBeNull();
        retrieved!.Title.Should().Be("Test Project");
        retrieved.CustomerId.Should().Be(123);
        retrieved.Status.Should().Be(ProjectStatus.Approved);
    }

    [Fact]
    public async Task AppDb_CanSaveProjectWithTasks()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Project with Tasks",
            CreatedBy = 456
        };

        var task1 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Task 1",
            ServiceType = "Repair",
            Status = TaskStatus.Requested
        };

        var task2 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Task 2",
            ServiceType = "Maintenance",
            Status = TaskStatus.Accepted
        };

        project.Tasks.Add(task1);
        project.Tasks.Add(task2);

        // Act
        _context.Projects.Add(project);
        await _context.SaveChangesAsync();

        // Assert
        var retrieved = await _context.Projects
            .Include(p => p.Tasks)
            .FirstOrDefaultAsync(p => p.ProjectId == project.ProjectId);

        retrieved.Should().NotBeNull();
        retrieved!.Tasks.Should().HaveCount(2);
        retrieved.Tasks.Should().Contain(t => t.Title == "Task 1");
        retrieved.Tasks.Should().Contain(t => t.Title == "Task 2");
    }

    [Fact]
    public async Task AppDb_CascadeDeleteTasksWhenProjectDeleted()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Project to Delete",
            CreatedBy = 456
        };

        var task = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Task to be deleted",
            ServiceType = "Repair"
        };

        project.Tasks.Add(task);
        _context.Projects.Add(project);
        await _context.SaveChangesAsync();

        // Act
        _context.Projects.Remove(project);
        await _context.SaveChangesAsync();

        // Assert
        var retrievedProject = await _context.Projects.FindAsync(project.ProjectId);
        var retrievedTask = await _context.Tasks.FindAsync(task.TaskId);

        retrievedProject.Should().BeNull();
        retrievedTask.Should().BeNull();
    }

    [Fact]
    public async Task AppDb_CanSaveProjectActivity()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Project with Activity",
            CreatedBy = 456
        };

        var activity = new ProjectActivity
        {
            Id = 1L,
            ProjectId = project.ProjectId,
            ActorId = 789L,
            ActorRole = "Admin",
            Message = "Project created",
            CreatedAt = DateTimeOffset.UtcNow
        };

        project.Activity.Add(activity);

        // Act
        _context.Projects.Add(project);
        await _context.SaveChangesAsync();

        // Assert
        var retrieved = await _context.Projects
            .Include(p => p.Activity)
            .FirstOrDefaultAsync(p => p.ProjectId == project.ProjectId);

        retrieved.Should().NotBeNull();
        retrieved!.Activity.Should().HaveCount(1);
        retrieved.Activity.First().Message.Should().Be("Project created");
        retrieved.Activity.First().ActorRole.Should().Be("Admin");
    }

    [Fact]
    public async Task AppDb_CanQueryProjectsByCustomerId()
    {
        // Arrange
        var customerId = 123L;
        var project1 = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = customerId,
            VehicleId = Guid.NewGuid(),
            Title = "Project 1",
            CreatedBy = 456
        };

        var project2 = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = customerId,
            VehicleId = Guid.NewGuid(),
            Title = "Project 2",
            CreatedBy = 456
        };

        var project3 = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 999,
            VehicleId = Guid.NewGuid(),
            Title = "Other Customer Project",
            CreatedBy = 456
        };

        _context.Projects.AddRange(project1, project2, project3);
        await _context.SaveChangesAsync();

        // Act
        var results = await _context.Projects
            .Where(p => p.CustomerId == customerId)
            .ToListAsync();

        // Assert
        results.Should().HaveCount(2);
        results.Should().Contain(p => p.Title == "Project 1");
        results.Should().Contain(p => p.Title == "Project 2");
        results.Should().NotContain(p => p.Title == "Other Customer Project");
    }

    [Fact]
    public async Task AppDb_CanQueryTasksByStatus()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Project",
            CreatedBy = 456
        };

        _context.Projects.Add(project);

        var task1 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "In Progress Task",
            ServiceType = "Repair",
            Status = TaskStatus.InProgress
        };

        var task2 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Completed Task",
            ServiceType = "Maintenance",
            Status = TaskStatus.Completed
        };

        _context.Tasks.AddRange(task1, task2);
        await _context.SaveChangesAsync();

        // Act
        var inProgressTasks = await _context.Tasks
            .Where(t => t.Status == TaskStatus.InProgress)
            .ToListAsync();

        // Assert
        inProgressTasks.Should().HaveCount(1);
        inProgressTasks.First().Title.Should().Be("In Progress Task");
    }

    [Fact]
    public async Task AppDb_CanQueryTasksByAssignee()
    {
        // Arrange
        var assigneeId = 789L;
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Project",
            CreatedBy = 456
        };

        _context.Projects.Add(project);

        var task1 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Assigned Task",
            ServiceType = "Repair",
            AssigneeId = assigneeId
        };

        var task2 = new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = project.ProjectId,
            Title = "Unassigned Task",
            ServiceType = "Maintenance",
            AssigneeId = null
        };

        _context.Tasks.AddRange(task1, task2);
        await _context.SaveChangesAsync();

        // Act
        var assignedTasks = await _context.Tasks
            .Where(t => t.AssigneeId == assigneeId)
            .ToListAsync();

        // Assert
        assignedTasks.Should().HaveCount(1);
        assignedTasks.First().Title.Should().Be("Assigned Task");
    }

    [Fact]
    public async Task AppDb_ProjectStatusStoredAsString()
    {
        // Arrange
        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = 123,
            VehicleId = Guid.NewGuid(),
            Title = "Test Project",
            Status = ProjectStatus.InProgress,
            CreatedBy = 456
        };

        // Act
        _context.Projects.Add(project);
        await _context.SaveChangesAsync();

        // Assert
        var retrieved = await _context.Projects.FindAsync(project.ProjectId);
        retrieved.Should().NotBeNull();
        retrieved!.Status.Should().Be(ProjectStatus.InProgress);
    }

    public void Dispose()
    {
        _context.Dispose();
    }
}
