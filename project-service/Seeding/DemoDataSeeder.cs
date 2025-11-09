using System.Linq;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;

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

        _logger.LogInformation("Seeding demo projects and tasks...");

        var now = DateTimeOffset.UtcNow;

        var employees = new[]
        {
            Guid.Parse("f381a9f7-13e5-4d44-83fd-637e77a9cc10"), // Alex Rivera
            Guid.Parse("6c9b6ef0-4c33-419f-bab9-1f0fb4ae1fcb"), // Priya Das
            Guid.Parse("a7a63a74-5fe6-4a48-8459-1fb328e0b2c5")  // Liam Chen
        };

        var customers = new[]
        {
            Guid.Parse("6e732997-2d4c-4763-b38d-8f07eb2a3d8c"),
            Guid.Parse("df88b339-8ecf-4fbf-9310-b01730f0d522"),
            Guid.Parse("4ba0e426-c88b-4e89-913f-8cf64a78a9d7")
        };

        var vehicles = new[]
        {
            Guid.Parse("b5c0b4aa-1f7c-49d4-9e8c-68d7a4b3fa7c"),
            Guid.Parse("a38bd5c4-8274-4d68-b9f5-a0bf095c8b01"),
            Guid.Parse("51f4c3ff-3a4b-4451-8be6-e96a5a165dce")
        };

        var diagnosticsProjectId = Guid.Parse("9f8ecf78-e431-4a66-9edf-34472f73a2de");
        var storefrontProjectId = Guid.Parse("c5885a7b-598c-4f74-9e2c-4d1e5f1d7a4c");
        var detailingProjectId = Guid.Parse("f5f1ddfd-d676-4f6a-8753-0464a7cbe9b9");

        var diagnosticsProject = new Project
        {
            ProjectId = diagnosticsProjectId,
            CustomerId = customers[0],
            VehicleId = vehicles[0],
            Title = "Fleet Truck Overhaul",
            Status = ProjectStatus.InProgress,
            CreatedAt = now.AddDays(-25),
            UpdatedAt = now.AddDays(-1),
            Budget = 18500m,
            DueDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(18)),
            ClientRequestId = "demo-truck-overhaul",
            Tasks = new List<TaskItem>
            {
                new()
                {
                    TaskId = Guid.Parse("0a7f1f0e-f9c9-4ad7-a115-96dcc8a1f30f"),
                    ProjectId = diagnosticsProjectId,
                    Title = "Engine diagnostics",
                    EstimateHours = 12m,
                    AssigneeId = employees[0],
                    Status = "IN_PROGRESS"
                },
                new()
                {
                    TaskId = Guid.Parse("dd862aef-866a-44c0-85ab-e2f6b483e7a5"),
                    ProjectId = diagnosticsProjectId,
                    Title = "Suspension upgrade",
                    EstimateHours = 18m,
                    AssigneeId = employees[1],
                    Status = "NOT_STARTED"
                },
                new()
                {
                    TaskId = Guid.Parse("3ad3b354-3d4f-49c5-9d30-7b4afda6a2ac"),
                    ProjectId = diagnosticsProjectId,
                    Title = "Quality control ride",
                    EstimateHours = 6m,
                    AssigneeId = employees[2],
                    Status = "BLOCKED"
                }
            }
        };

        var storefrontProject = new Project
        {
            ProjectId = storefrontProjectId,
            CustomerId = customers[1],
            VehicleId = vehicles[1],
            Title = "Customer Experience Portal",
            Status = ProjectStatus.Approved,
            CreatedAt = now.AddDays(-14),
            UpdatedAt = now.AddDays(-3),
            Budget = 42000m,
            DueDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(40)),
            ClientRequestId = "demo-portal-build",
            Tasks = new List<TaskItem>
            {
                new()
                {
                    TaskId = Guid.Parse("a8d79b1a-5f77-4a2b-9b39-c5e6bb0f5cbf"),
                    ProjectId = storefrontProjectId,
                    Title = "Wireframes & flows",
                    EstimateHours = 24m,
                    AssigneeId = employees[2],
                    Status = "COMPLETE"
                },
                new()
                {
                    TaskId = Guid.Parse("948fe1b6-d74c-4871-8a33-2f2c10bb3e23"),
                    ProjectId = storefrontProjectId,
                    Title = "Frontend build",
                    EstimateHours = 60m,
                    AssigneeId = employees[1],
                    Status = "IN_PROGRESS"
                },
                new()
                {
                    TaskId = Guid.Parse("d91d796f-b49a-45d0-92d3-26989d0b2ebc"),
                    ProjectId = storefrontProjectId,
                    Title = "API integration",
                    EstimateHours = 48m,
                    AssigneeId = employees[0],
                    Status = "NOT_STARTED"
                }
            }
        };

        var detailingProject = new Project
        {
            ProjectId = detailingProjectId,
            CustomerId = customers[2],
            VehicleId = vehicles[2],
            Title = "Showroom Detailing",
            Status = ProjectStatus.Completed,
            CreatedAt = now.AddDays(-8),
            UpdatedAt = now.AddDays(-2),
            Budget = 2200m,
            DueDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(-1)),
            ClientRequestId = "demo-detailing-2025",
            Tasks = new List<TaskItem>
            {
                new()
                {
                    TaskId = Guid.Parse("1e1fb3be-1b2b-4d53-abb0-9a3838dbe65d"),
                    ProjectId = detailingProjectId,
                    Title = "Paint correction",
                    EstimateHours = 8m,
                    AssigneeId = employees[0],
                    Status = "COMPLETE"
                },
                new()
                {
                    TaskId = Guid.Parse("efe6240c-46f6-4da8-92d1-3f2262cb3855"),
                    ProjectId = detailingProjectId,
                    Title = "Interior detailing",
                    EstimateHours = 5m,
                    AssigneeId = employees[1],
                    Status = "COMPLETE"
                }
            }
        };

        var projects = new[] { diagnosticsProject, storefrontProject, detailingProject };

        var statusHistory = new List<StatusHistory>();

        statusHistory.AddRange(CreateHistory(
            diagnosticsProjectId,
            (ProjectStatus.Requested, ProjectStatus.Quoted, now.AddDays(-24), employees[0], "Initial inspection complete."),
            (ProjectStatus.Quoted, ProjectStatus.Approved, now.AddDays(-22), employees[0], "Quote accepted by customer."),
            (ProjectStatus.Approved, ProjectStatus.InProgress, now.AddDays(-20), employees[1], "Technicians scheduled.")
        ));

        statusHistory.AddRange(CreateHistory(
            storefrontProjectId,
            (ProjectStatus.Requested, ProjectStatus.Quoted, now.AddDays(-13), employees[2], "Requirements gathered."),
            (ProjectStatus.Quoted, ProjectStatus.Approved, now.AddDays(-11), employees[2], "Statement of work signed.")
        ));

        statusHistory.AddRange(CreateHistory(
            detailingProjectId,
            (ProjectStatus.Requested, ProjectStatus.Approved, now.AddDays(-7), employees[1], "Fast-tracked."),
            (ProjectStatus.Approved, ProjectStatus.InProgress, now.AddDays(-6), employees[1], "Detailing started."),
            (ProjectStatus.InProgress, ProjectStatus.Completed, now.AddDays(-2), employees[0], "Delivered to showroom.")
        ));

        await _db.Projects.AddRangeAsync(projects, cancellationToken);
        await _db.StatusHistory.AddRangeAsync(statusHistory, cancellationToken);
        await _db.SaveChangesAsync(cancellationToken);

        var taskCount = projects.Sum(p => p.Tasks.Count);
        _logger.LogInformation("Seeded {ProjectCount} demo projects with {TaskCount} tasks.", projects.Length, taskCount);
    }

    private static IEnumerable<StatusHistory> CreateHistory(Guid projectId, params (ProjectStatus From, ProjectStatus To, DateTimeOffset At, Guid Actor, string? Note)[] entries)
    {
        foreach (var entry in entries)
        {
            yield return new StatusHistory
            {
                ProjectId = projectId,
                FromStatus = entry.From,
                ToStatus = entry.To,
                ChangedAt = entry.At,
                ChangedBy = entry.Actor,
                Note = entry.Note
            };
        }
    }
}
