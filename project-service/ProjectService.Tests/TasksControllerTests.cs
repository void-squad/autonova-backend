using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Threading;
using System.Threading.Tasks;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Controllers;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using TaskStatus = ProjectService.Domain.Enums.TaskStatus;
using ProjectService.Dtos;
using ProjectService.Validators;
using Xunit;

namespace ProjectService.Tests;

public class TasksControllerTests
{
    private readonly AppDb _db;
    private readonly TasksController _controller;

    public TasksControllerTests()
    {
        var options = new DbContextOptionsBuilder<AppDb>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString()).Options;

        _db = new AppDb(options);
        _controller = new TasksController(_db, new UpdateTaskStatusRequestValidator(), new CreateProjectTaskRequestValidator());

        var claims = new[] { new Claim("sub", "900"), new Claim("role", "employee") };
        _controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext { User = new ClaimsPrincipal(new ClaimsIdentity(claims, "test")) } };
    }

    [Fact]
    public async Task UpdateStatus_Forbidden_WhenNotAssignee()
    {
        var project = new Project { ProjectId = Guid.NewGuid(), Title = "P", CreatedAt = DateTimeOffset.UtcNow, UpdatedAt = DateTimeOffset.UtcNow };
        var task = new ProjectTask { TaskId = Guid.NewGuid(), ProjectId = project.ProjectId, Title = "T", ServiceType = "S", AssigneeId = 111, CreatedAt = DateTimeOffset.UtcNow, UpdatedAt = DateTimeOffset.UtcNow };

        _db.Projects.Add(project);
        _db.Tasks.Add(task);
        await _db.SaveChangesAsync();

        var result = await _controller.UpdateStatus(task.TaskId, new UpdateTaskStatusRequest { Status = TaskStatus.InProgress }, CancellationToken.None);
        result.Should().BeOfType<ForbidResult>();
    }

    [Fact]
    public async Task CreateTask_CreatesTask_ForAdmin()
    {
        // switch user role to admin
        var claims = new[] { new Claim("sub", "1"), new Claim("role", "admin") };
        _controller.ControllerContext.HttpContext = new DefaultHttpContext { User = new ClaimsPrincipal(new ClaimsIdentity(claims, "test")) };

        var project = new Project { ProjectId = Guid.NewGuid(), Title = "P", CreatedAt = DateTimeOffset.UtcNow, UpdatedAt = DateTimeOffset.UtcNow };
        _db.Projects.Add(project);
        await _db.SaveChangesAsync();

        var req = new CreateProjectTaskRequest { Title = "New", ServiceType = "Type", Detail = "D" };
        var result = await _controller.CreateTask(project.ProjectId, req, CancellationToken.None);
        result.Result.Should().BeOfType<CreatedAtActionResult>();
        _db.Tasks.Count(t => t.ProjectId == project.ProjectId).Should().Be(1);
    }
}
