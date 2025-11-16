using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Http.Extensions;
using ProjectService.Dtos;

namespace ProjectService.Services;

public class AppointmentServiceClient : IAppointmentServiceClient
{
    private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    private readonly HttpClient _httpClient;
    private readonly ILogger<AppointmentServiceClient> _logger;

    public AppointmentServiceClient(HttpClient httpClient, ILogger<AppointmentServiceClient> logger)
    {
        _httpClient = httpClient;
        _logger = logger;
    }

    public async Task<IReadOnlyList<ExternalAppointmentDto>> GetAdminAppointmentsAsync(
        string? status,
        DateTimeOffset? from,
        DateTimeOffset? to,
        Guid? vehicleId,
        CancellationToken cancellationToken)
    {
        var query = new QueryBuilder();
        if (!string.IsNullOrWhiteSpace(status))
        {
            query.Add("status", status);
        }

        if (from.HasValue)
        {
            query.Add("from", from.Value.ToString("O"));
        }

        if (to.HasValue)
        {
            query.Add("to", to.Value.ToString("O"));
        }

        if (vehicleId.HasValue)
        {
            query.Add("vehicleId", vehicleId.Value.ToString());
        }

        var url = "admin" + query.ToQueryString();
        try
        {
            var response = await _httpClient.GetAsync(url, cancellationToken);
            if (response.StatusCode == System.Net.HttpStatusCode.NotFound)
            {
                return Array.Empty<ExternalAppointmentDto>();
            }

            response.EnsureSuccessStatusCode();
            var payload = await response.Content.ReadFromJsonAsync<IReadOnlyList<ExternalAppointmentDto>>(SerializerOptions, cancellationToken);
            return payload ?? Array.Empty<ExternalAppointmentDto>();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to retrieve appointments from appointment service.");
            throw;
        }
    }

    public async Task<ExternalAppointmentDto?> GetAppointmentAsync(Guid id, CancellationToken cancellationToken)
    {
        try
        {
            var response = await _httpClient.GetAsync($"{id}", cancellationToken);
            if (response.StatusCode == System.Net.HttpStatusCode.NotFound)
            {
                return null;
            }

            response.EnsureSuccessStatusCode();
            return await response.Content.ReadFromJsonAsync<ExternalAppointmentDto>(SerializerOptions, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to fetch appointment {AppointmentId} from appointment service.", id);
            throw;
        }
    }

    public async Task UpdateAppointmentStatusAsync(Guid id, UpdateAppointmentStatusRequest request, CancellationToken cancellationToken)
    {
        try
        {
            using var content = JsonContent.Create(request, options: SerializerOptions);
            var message = new HttpRequestMessage(HttpMethod.Patch, $"{id}/status")
            {
                Content = content
            };
            var response = await _httpClient.SendAsync(message, cancellationToken);
            response.EnsureSuccessStatusCode();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to update appointment {AppointmentId} status.", id);
            throw;
        }
    }
}
