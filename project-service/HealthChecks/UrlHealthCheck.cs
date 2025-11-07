using System.Net.Http;
using Microsoft.Extensions.Diagnostics.HealthChecks;

namespace ProjectService.HealthChecks;

public class UrlHealthCheck : IHealthCheck
{
    private readonly string _url;
    private readonly IHttpClientFactory _httpClientFactory;

    public UrlHealthCheck(string url, IHttpClientFactory httpClientFactory)
    {
        _url = url ?? throw new ArgumentNullException(nameof(url));
        _httpClientFactory = httpClientFactory;
    }

    public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default)
    {
        try
        {
            var client = _httpClientFactory.CreateClient("healthchecks");
            using var response = await client.GetAsync(_url, cancellationToken);
            if (response.IsSuccessStatusCode)
            {
                return HealthCheckResult.Healthy();
            }

            return HealthCheckResult.Degraded($"Status {(int)response.StatusCode}");
        }
        catch (Exception ex)
        {
            return HealthCheckResult.Degraded(ex.Message);
        }
    }
}
