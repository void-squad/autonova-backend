using ProjectService.Dtos;

namespace ProjectService.Services;

public interface IAppointmentServiceClient
{
    Task<IReadOnlyList<ExternalAppointmentDto>> GetAdminAppointmentsAsync(
        string? status,
        DateTimeOffset? from,
        DateTimeOffset? to,
        CancellationToken cancellationToken);

    Task<ExternalAppointmentDto?> GetAppointmentAsync(Guid id, CancellationToken cancellationToken);

    Task UpdateAppointmentStatusAsync(Guid id, UpdateAppointmentStatusRequest request, CancellationToken cancellationToken);
}
