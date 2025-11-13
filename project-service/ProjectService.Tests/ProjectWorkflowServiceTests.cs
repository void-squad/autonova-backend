using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;
using ProjectService.Dtos.ChangeRequests;
using ProjectService.Services;
using Xunit;

namespace ProjectService.Tests;

public class ProjectWorkflowServiceTests
{
    [Fact]
    public async Task StatusHistory_ReturnsDescendingOrder()
    {
        await using var context = CreateContext();
        var service = CreateService(context);
        var projectId = Guid.NewGuid();
        context.Projects.Add(new Project
        {
            ProjectId = projectId,
            CustomerId = 1001,
            VehicleId = Guid.NewGuid(),
            Title = "History Test",
            Status = ProjectStatus.Requested,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        });
        context.StatusHistory.AddRange(
            new StatusHistory
            {
                ProjectId = projectId,
                FromStatus = ProjectStatus.Requested,
                ToStatus = ProjectStatus.Quoted,
                ChangedBy = Guid.NewGuid(),
                ChangedAt = DateTimeOffset.UtcNow.AddMinutes(-10)
            },
            new StatusHistory
            {
                ProjectId = projectId,
                FromStatus = ProjectStatus.Quoted,
                ToStatus = ProjectStatus.Approved,
                ChangedBy = Guid.NewGuid(),
                ChangedAt = DateTimeOffset.UtcNow
            });
        await context.SaveChangesAsync();

        var history = await service.GetStatusHistoryAsync(projectId, CancellationToken.None);

        history.Should().HaveCount(2);
        history.Select(h => h.ChangedAt).Should().BeInDescendingOrder();
    }

    [Fact]
    public async Task CreateProject_IsIdempotentWithSameKey()
    {
        await using var context = CreateContext();
        var service = CreateService(context);
        var request = new CreateProjectRequest
        {
            CustomerId = 2001,
            VehicleId = Guid.NewGuid(),
            Title = "Idempotent Project"
        };
        var key = Guid.NewGuid().ToString("N");
        var actorId = Guid.NewGuid();

        var first = await service.CreateProjectAsync(request, actorId, "customer", key, CancellationToken.None);
        var second = await service.CreateProjectAsync(request, actorId, "customer", key, CancellationToken.None);

        first.ProjectId.Should().Be(second.ProjectId);
        (await context.Projects.CountAsync()).Should().Be(1);
    }

    [Fact]
    public async Task ApproveThenApplyChangeRequest_UpdatesProjectAndOutbox()
    {
        await using var context = CreateContext();
        var service = CreateService(context);
        var actorId = Guid.NewGuid();

        var project = await service.CreateProjectAsync(new CreateProjectRequest
        {
            CustomerId = 3001,
            VehicleId = Guid.NewGuid(),
            Title = "Scope Extensions"
        }, actorId, "customer", Guid.NewGuid().ToString("N"), CancellationToken.None);

        var changeRequest = await service.CreateChangeRequestAsync(project.ProjectId, new CreateChangeRequestRequest
        {
            Title = "Add mobile app",
            ProposedPriceDelta = 5000m,
            ProposedExtraHours = 10,
            ProposedNewDueDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(14))
        }, actorId, "customer", Guid.NewGuid().ToString("N"), CancellationToken.None);

        await service.ApproveChangeRequestAsync(changeRequest.ChangeRequestId, 0, actorId, "manager", Guid.NewGuid().ToString("N"), CancellationToken.None);
        var approved = await context.ChangeRequests.AsNoTracking().FirstAsync();
        var priorUpdatedEvents = context.Outbox.Count(o => o.Topic == "project.updated");

        var applied = await service.ApplyChangeRequestAsync(approved.ChangeRequestId, 0, actorId, "manager", Guid.NewGuid().ToString("N"), CancellationToken.None);

        applied.Status.Should().Be(ChangeRequestStatus.Applied);
        var reloaded = await context.Projects.FirstAsync(p => p.ProjectId == project.ProjectId);
        reloaded.Budget.Should().Be(5000m);
        context.Tasks.Count(t => t.ProjectId == project.ProjectId).Should().Be(1);
        context.Outbox.Count(o => o.Topic == "project.change-request.applied").Should().Be(1);
        context.Outbox.Count(o => o.Topic == "project.updated").Should().Be(priorUpdatedEvents + 1);
    }

    private static AppDb CreateContext()
    {
        var options = new DbContextOptionsBuilder<AppDb>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new AppDb(options);
    }

    private static ProjectWorkflowService CreateService(AppDb context)
    {
        return new ProjectWorkflowService(context, NullLogger<ProjectWorkflowService>.Instance);
    }
}
