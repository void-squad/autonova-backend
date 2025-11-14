using Microsoft.AspNetCore.Http;
using ProjectService.Domain.Exceptions;

namespace ProjectService.Extensions;

public static class HttpRequestExtensions
{
    private const string HeaderName = "X-Idempotency-Key";

    public static string? GetIdempotencyKey(this HttpRequest request)
    {
        if (!request.Headers.TryGetValue(HeaderName, out var values))
        {
            return null;
        }

        var key = values.FirstOrDefault()?.Trim();
        if (string.IsNullOrWhiteSpace(key))
        {
            return null;
        }

        if (key.Length > 64)
        {
            throw new DomainException("X-Idempotency-Key cannot exceed 64 characters.");
        }

        return key;
    }
}
