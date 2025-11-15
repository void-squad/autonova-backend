using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using TaskStatusEnum = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Seeding;

public class DemoDataSeeder
{
    private readonly AppDb _db;
    private readonly ILogger<DemoDataSeeder> _logger;

    public DemoDataSeeder(AppDb db, ILogger<DemoDataSeeder> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task SeedAsync(CancellationToken cancellationToken = default)
    {
        if (await _db.Projects.AsNoTracking().AnyAsync(cancellationToken))
        {
            _logger.LogInformation("Project data already present; skipping demo seed.");
            return;
        }

        var now = DateTimeOffset.UtcNow;
        const long demoCustomer = 1001;
        var demoVehicle = Guid.Parse("d0970b0a-4b52-47cb-838d-7c306114dd0f");

        var project = new Project
        {
            ProjectId = Guid.NewGuid(),
            CustomerId = demoCustomer,
            VehicleId = demoVehicle,
            Title = "Interior customization",
            Description = "Leather interior refresh, infotainment upgrade, sound dampening.",
            Status = ProjectStatus.InProgress,
            RequestedStart = now.AddDays(-10),
            RequestedEnd = now.AddDays(5),
            ApprovedStart = now.AddDays(-7),
            ApprovedEnd = now.AddDays(3),
            CreatedAt = now.AddDays(-12),
            UpdatedAt = now.AddDays(-1),
            CreatedBy = demoCustomer
        };

        var tasks = new[]
        {
            new ProjectTask
            {
                TaskId = Guid.NewGuid(),
                ProjectId = project.ProjectId,
                Title = "Custom upholstery",
                ServiceType = "Interior",
                Detail = "Replace seats and dashboard with premium leather.",
                Status = TaskStatusEnum.InProgress,
                AssigneeId = 2001,
                ScheduledStart = now.AddDays(-6),
                ScheduledEnd = now.AddDays(-2),
                CreatedAt = now.AddDays(-11),
                UpdatedAt = now.AddDays(-1)
            },
            new ProjectTask
            {
                TaskId = Guid.NewGuid(),
                ProjectId = project.ProjectId,
                Title = "Infotainment upgrade",
                ServiceType = "Electronics",
                Detail = "Install new head unit with wireless CarPlay.",
                Status = TaskStatusEnum.Accepted,
                AssigneeId = 2002,
                ScheduledStart = now.AddDays(-1),
                ScheduledEnd = now.AddDays(1),
                CreatedAt = now.AddDays(-10),
                UpdatedAt = now.AddDays(-1)
            }
        };

        var activity = new[]
        {
            new ProjectActivity
            {
                ProjectId = project.ProjectId,
                ActorId = demoCustomer,
                ActorRole = "customer",
                Message = "Project requested by customer",
                CreatedAt = now.AddDays(-12)
            },
            new ProjectActivity
            {
                ProjectId = project.ProjectId,
                ActorId = 9000,
                ActorRole = "admin",
                Message = "Project approved",
                CreatedAt = now.AddDays(-7)
            }
        };

        await _db.Projects.AddAsync(project, cancellationToken);
        await _db.Tasks.AddRangeAsync(tasks, cancellationToken);
        await _db.Activity.AddRangeAsync(activity, cancellationToken);
        await _db.SaveChangesAsync(cancellationToken);
    }
}
