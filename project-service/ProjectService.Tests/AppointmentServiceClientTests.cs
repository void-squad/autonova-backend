using System;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using FluentAssertions;
using Microsoft.Extensions.Logging;
using Moq;
using Moq.Protected;
using ProjectService.Dtos;
using ProjectService.Services;
using Xunit;

namespace ProjectService.Tests;

public class AppointmentServiceClientTests
{
    private readonly Mock<ILogger<AppointmentServiceClient>> _loggerMock;
    private readonly Mock<HttpMessageHandler> _httpMessageHandlerMock;
    private readonly HttpClient _httpClient;
    private readonly AppointmentServiceClient _client;

    public AppointmentServiceClientTests()
    {
        _loggerMock = new Mock<ILogger<AppointmentServiceClient>>();
        _httpMessageHandlerMock = new Mock<HttpMessageHandler>();
        _httpClient = new HttpClient(_httpMessageHandlerMock.Object)
        {
            BaseAddress = new Uri("http://localhost/api/appointments/")
        };
        _client = new AppointmentServiceClient(_httpClient, _loggerMock.Object);
    }

    [Fact]
    public async Task GetAdminAppointmentsAsync_WithNoFilters_ReturnsAppointments()
    {
        // Arrange
        var appointments = new[]
        {
            new ExternalAppointmentDto { Id = Guid.NewGuid() },
            new ExternalAppointmentDto { Id = Guid.NewGuid() }
        };
        var json = JsonSerializer.Serialize(appointments);

        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent(json)
            });

        // Act
        var result = await _client.GetAdminAppointmentsAsync(null, null, null, null, CancellationToken.None);

        // Assert
        result.Should().HaveCount(2);
    }

    [Fact]
    public async Task GetAdminAppointmentsAsync_WithStatusFilter_BuildsCorrectQuery()
    {
        // Arrange
        var appointments = Array.Empty<ExternalAppointmentDto>();
        var json = JsonSerializer.Serialize(appointments);
        HttpRequestMessage? capturedRequest = null;

        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .Callback<HttpRequestMessage, CancellationToken>((req, _) => capturedRequest = req)
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent(json)
            });

        // Act
        await _client.GetAdminAppointmentsAsync("confirmed", null, null, null, CancellationToken.None);

        // Assert
        capturedRequest.Should().NotBeNull();
        capturedRequest!.RequestUri!.Query.Should().Contain("status=confirmed");
    }

    [Fact]
    public async Task GetAdminAppointmentsAsync_WithDateFilters_BuildsCorrectQuery()
    {
        // Arrange
        var from = new DateTimeOffset(2024, 1, 1, 0, 0, 0, TimeSpan.Zero);
        var to = new DateTimeOffset(2024, 12, 31, 23, 59, 59, TimeSpan.Zero);
        var appointments = Array.Empty<ExternalAppointmentDto>();
        var json = JsonSerializer.Serialize(appointments);
        HttpRequestMessage? capturedRequest = null;

        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .Callback<HttpRequestMessage, CancellationToken>((req, _) => capturedRequest = req)
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent(json)
            });

        // Act
        await _client.GetAdminAppointmentsAsync(null, from, to, null, CancellationToken.None);

        // Assert
        capturedRequest.Should().NotBeNull();
        capturedRequest!.RequestUri!.Query.Should().Contain("from=");
        capturedRequest.RequestUri.Query.Should().Contain("to=");
    }

    [Fact]
    public async Task GetAdminAppointmentsAsync_WithVehicleIdFilter_BuildsCorrectQuery()
    {
        // Arrange
        var vehicleId = Guid.NewGuid();
        var appointments = Array.Empty<ExternalAppointmentDto>();
        var json = JsonSerializer.Serialize(appointments);
        HttpRequestMessage? capturedRequest = null;

        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .Callback<HttpRequestMessage, CancellationToken>((req, _) => capturedRequest = req)
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent(json)
            });

        // Act
        await _client.GetAdminAppointmentsAsync(null, null, null, vehicleId, CancellationToken.None);

        // Assert
        capturedRequest.Should().NotBeNull();
        capturedRequest!.RequestUri!.Query.Should().Contain($"vehicleId={vehicleId}");
    }

    [Fact]
    public async Task GetAdminAppointmentsAsync_WhenNotFound_ReturnsEmptyList()
    {
        // Arrange
        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.NotFound
            });

        // Act
        var result = await _client.GetAdminAppointmentsAsync(null, null, null, null, CancellationToken.None);

        // Assert
        result.Should().BeEmpty();
    }

    [Fact]
    public async Task GetAdminAppointmentsAsync_WhenError_ThrowsException()
    {
        // Arrange
        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.InternalServerError
            });

        // Act
        Func<Task> act = async () => await _client.GetAdminAppointmentsAsync(null, null, null, null, CancellationToken.None);

        // Assert
        await act.Should().ThrowAsync<HttpRequestException>();
    }

    [Fact]
    public async Task GetAppointmentAsync_WithValidId_ReturnsAppointment()
    {
        // Arrange
        var appointmentId = Guid.NewGuid();
        var appointment = new ExternalAppointmentDto { Id = appointmentId };
        var json = JsonSerializer.Serialize(appointment);

        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent(json)
            });

        // Act
        var result = await _client.GetAppointmentAsync(appointmentId, CancellationToken.None);

        // Assert
        result.Should().NotBeNull();
        result!.Id.Should().Be(appointmentId);
    }

    [Fact]
    public async Task GetAppointmentAsync_WhenNotFound_ReturnsNull()
    {
        // Arrange
        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.NotFound
            });

        // Act
        var result = await _client.GetAppointmentAsync(Guid.NewGuid(), CancellationToken.None);

        // Assert
        result.Should().BeNull();
    }

    [Fact]
    public async Task GetAppointmentAsync_WhenError_ThrowsException()
    {
        // Arrange
        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.InternalServerError
            });

        // Act
        Func<Task> act = async () => await _client.GetAppointmentAsync(Guid.NewGuid(), CancellationToken.None);

        // Assert
        await act.Should().ThrowAsync<HttpRequestException>();
    }

    [Fact]
    public async Task UpdateAppointmentStatusAsync_WithValidRequest_SendsPatchRequest()
    {
        // Arrange
        var appointmentId = Guid.NewGuid();
        var request = new UpdateAppointmentStatusRequest { Status = "completed" };
        HttpRequestMessage? capturedRequest = null;

        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .Callback<HttpRequestMessage, CancellationToken>((req, _) => capturedRequest = req)
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.NoContent
            });

        // Act
        await _client.UpdateAppointmentStatusAsync(appointmentId, request, CancellationToken.None);

        // Assert
        capturedRequest.Should().NotBeNull();
        capturedRequest!.Method.Should().Be(HttpMethod.Patch);
        capturedRequest.RequestUri!.ToString().Should().Contain($"{appointmentId}/status");
    }

    [Fact]
    public async Task UpdateAppointmentStatusAsync_WhenError_ThrowsException()
    {
        // Arrange
        _httpMessageHandlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.BadRequest
            });

        // Act
        Func<Task> act = async () => await _client.UpdateAppointmentStatusAsync(
            Guid.NewGuid(),
            new UpdateAppointmentStatusRequest { Status = "completed" },
            CancellationToken.None);

        // Assert
        await act.Should().ThrowAsync<HttpRequestException>();
    }
}
