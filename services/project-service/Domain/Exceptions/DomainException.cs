using Microsoft.AspNetCore.Http;

namespace ProjectService.Domain.Exceptions;

public class DomainException(string message, int statusCode = StatusCodes.Status400BadRequest) : Exception(message)
{
    public int StatusCode { get; } = statusCode;
}
