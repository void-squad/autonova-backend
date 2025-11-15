using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;
using ProjectService.Extensions;
using ProjectService.Services;
using TaskStatusEnum = ProjectService.Domain.Enums.TaskStatus;

namespace ProjectService.Controllers;

[ApiController]
[Route("api/admin/appointments")]
[Authorize(Policy = "AdminOnly")]
public class AdminAppointmentsController : ControllerBase
{
    private readonly IAppointmentServiceClient _appointmentClient;
    private readonly AppDb _db;
    private readonly ILogger<AdminAppointmentsController> _logger;

    public AdminAppointmentsController(
        IAppointmentServiceClient appointmentClient,
        AppDb db,
        ILogger<AdminAppointmentsController> logger)
    {
        _appointmentClient = appointmentClient;
        _db = db;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> GetAppointments(
        [FromQuery] string? status,
        [FromQuery] DateTimeOffset? from,
        [FromQuery] DateTimeOffset? to,
        [FromQuery] Guid? vehicleId,
        CancellationToken cancellationToken)
    {
        var items = await _appointmentClient.GetAdminAppointmentsAsync(status, from, to, vehicleId, cancellationToken);
        return Ok(items);
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> GetAppointment(Guid id, CancellationToken cancellationToken)
    {
        var appointment = await _appointmentClient.GetAppointmentAsync(id, cancellationToken);
        return appointment is null ? NotFound() : Ok(appointment);
    }

    [HttpPatch("{id:guid}/status")]
    public async Task<IActionResult> UpdateStatus(Guid id, [FromBody] UpdateAppointmentStatusRequest request, CancellationToken cancellationToken)
    {
        await _appointmentClient.UpdateAppointmentStatusAsync(id, request, cancellationToken);
        return NoContent();
    }

    [HttpPost("{id:guid}/convert")]
    public async Task<ActionResult<ProjectDetailsDto>> ConvertToProject(
        Guid id,
        [FromBody] ConvertAppointmentRequest? request,
        CancellationToken cancellationToken)
    {
        var appointment = await _appointmentClient.GetAppointmentAsync(id, cancellationToken);
        if (appointment is null)
        {
            return NotFound();
        }

        var now = DateTimeOffset.UtcNow;
        Project? targetProject = null;
        bool createdProject = false;

        if (request?.ProjectId is not null)
        {
            targetProject = await _db.Projects
                .Include(p => p.Tasks)
                .Include(p => p.Activity)
                .FirstOrDefaultAsync(p => p.ProjectId == request.ProjectId.Value, cancellationToken);

            if (targetProject is null)
            {
                return NotFound(new { message = $"Project {request.ProjectId} not found." });
            }

            if (targetProject.VehicleId != appointment.VehicleId)
            {
                return BadRequest("The selected project belongs to a different vehicle.");
            }
        }
        else
        {
            targetProject = await _db.Projects
                .Include(p => p.Tasks)
                .Include(p => p.Activity)
                .FirstOrDefaultAsync(p => p.VehicleId == appointment.VehicleId, cancellationToken);

            if (targetProject is not null)
            {
                return Conflict(new
                {
                    code = "ProjectExists",
                    projectId = targetProject.ProjectId,
                    message = "A project already exists for this vehicle."
                });
            }

            var customerId = ConvertGuidToLong(appointment.CustomerId);
            targetProject = new Project
            {
                ProjectId = Guid.NewGuid(),
                CustomerId = customerId,
                VehicleId = appointment.VehicleId,
                Title = appointment.ServiceType,
                Description = appointment.Notes,
                Status = ProjectStatus.PendingReview,
                RequestedStart = appointment.StartTime,
                RequestedEnd = appointment.EndTime,
                AppointmentId = appointment.Id,
                AppointmentSnapshot = JsonSerializer.Serialize(appointment, new JsonSerializerOptions(JsonSerializerDefaults.Web)),
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = customerId
            };

            targetProject.Tasks.Add(CreateTaskFromAppointment(appointment, targetProject.ProjectId, now));
            targetProject.Activity.Add(new ProjectActivity
            {
                ActorId = customerId,
                ActorRole = "system",
                Message = $"Project created from appointment {appointment.Id}",
                CreatedAt = now
            });

            await _db.Projects.AddAsync(targetProject, cancellationToken);
            createdProject = true;
        }

        if (!createdProject)
        {
            var taskEntity = CreateTaskFromAppointment(appointment, targetProject.ProjectId, now);
            await _db.Tasks.AddAsync(taskEntity, cancellationToken);
            await _db.Activity.AddAsync(new ProjectActivity
            {
                ProjectId = targetProject.ProjectId,
                ActorId = ConvertGuidToLong(appointment.CustomerId),
                ActorRole = "system",
                Message = $"Appointment {appointment.Id} added as a task",
                CreatedAt = now
            }, cancellationToken);
            targetProject!.UpdatedAt = now;
        }

        await _db.SaveChangesAsync(cancellationToken);
        await TryUpdateAppointmentStatusAsync(id, targetProject.ProjectId, cancellationToken);

        var freshProject = await _db.Projects
            .Include(p => p.Tasks)
            .Include(p => p.Activity)
            .FirstAsync(p => p.ProjectId == targetProject.ProjectId, cancellationToken);

        var details = freshProject.ToDetails();
        if (createdProject)
        {
            return CreatedAtAction(
                nameof(AdminProjectsController.GetProject),
                "AdminProjects",
                new { projectId = freshProject.ProjectId },
                details);
        }

        return Ok(details);
    }

    private ProjectTask CreateTaskFromAppointment(ExternalAppointmentDto appt, Guid projectId, DateTimeOffset now)
    {
        return new ProjectTask
        {
            TaskId = Guid.NewGuid(),
            ProjectId = projectId,
            Title = appt.ServiceType,
            ServiceType = appt.ServiceType,
            Detail = appt.Notes,
            Status = MapAppointmentStatus(appt.Status),
            AssigneeId = appt.AssignedEmployeeId.HasValue ? ConvertGuidToLong(appt.AssignedEmployeeId.Value) : null,
            ScheduledStart = appt.StartTime,
            ScheduledEnd = appt.EndTime,
            AppointmentId = appt.Id,
            CreatedAt = now,
            UpdatedAt = now
        };
    }

    private async Task TryUpdateAppointmentStatusAsync(Guid appointmentId, Guid projectId, CancellationToken cancellationToken)
    {
        try
        {
            await _appointmentClient.UpdateAppointmentStatusAsync(
                appointmentId,
                new UpdateAppointmentStatusRequest
                {
                    Status = "IN_PROGRESS",
                    AdminNote = $"Converted to project {projectId}"
                },
                cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to update appointment {AppointmentId} after conversion.", appointmentId);
        }
    }

    private static TaskStatusEnum MapAppointmentStatus(string? status) =>
        (status ?? string.Empty).ToUpperInvariant() switch
        {
            "CONFIRMED" => TaskStatusEnum.Accepted,
            "IN_PROGRESS" => TaskStatusEnum.InProgress,
            "COMPLETED" => TaskStatusEnum.Completed,
            "CANCELLED" => TaskStatusEnum.Cancelled,
            _ => TaskStatusEnum.Requested
        };

    private static long ConvertGuidToLong(Guid value)
    {
        Span<byte> buffer = stackalloc byte[16];
        value.TryWriteBytes(buffer);
        return BitConverter.ToInt64(buffer[..8]);
    }
}
