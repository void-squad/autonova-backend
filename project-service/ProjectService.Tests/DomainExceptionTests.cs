using System;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using ProjectService.Domain.Exceptions;
using Xunit;

namespace ProjectService.Tests;

public class DomainExceptionTests
{
    [Fact]
    public void DomainException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new DomainException("Test error message");

        // Assert
        exception.Message.Should().Be("Test error message");
    }

    [Fact]
    public void DomainException_WithoutStatusCode_UsesDefaultBadRequest()
    {
        // Arrange & Act
        var exception = new DomainException("Test error");

        // Assert
        exception.StatusCode.Should().Be(StatusCodes.Status400BadRequest);
    }

    [Fact]
    public void DomainException_WithCustomStatusCode_SetsStatusCode()
    {
        // Arrange & Act
        var exception = new DomainException("Not found", StatusCodes.Status404NotFound);

        // Assert
        exception.StatusCode.Should().Be(StatusCodes.Status404NotFound);
        exception.Message.Should().Be("Not found");
    }

    [Fact]
    public void DomainException_WithConflictStatus_SetsCorrectly()
    {
        // Arrange & Act
        var exception = new DomainException("Conflict occurred", StatusCodes.Status409Conflict);

        // Assert
        exception.StatusCode.Should().Be(StatusCodes.Status409Conflict);
    }

    [Fact]
    public void DomainException_WithUnauthorizedStatus_SetsCorrectly()
    {
        // Arrange & Act
        var exception = new DomainException("Unauthorized", StatusCodes.Status401Unauthorized);

        // Assert
        exception.StatusCode.Should().Be(StatusCodes.Status401Unauthorized);
    }

    [Fact]
    public void DomainException_WithForbiddenStatus_SetsCorrectly()
    {
        // Arrange & Act
        var exception = new DomainException("Forbidden", StatusCodes.Status403Forbidden);

        // Assert
        exception.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
    }

    [Fact]
    public void DomainException_IsException_CanBeThrown()
    {
        // Arrange
        Action act = () => throw new DomainException("Test exception");

        // Act & Assert
        act.Should().Throw<DomainException>()
            .WithMessage("Test exception");
    }

    [Fact]
    public void DomainException_CanBeCaught_AsException()
    {
        // Arrange
        Exception? caughtException = null;

        try
        {
            throw new DomainException("Test", StatusCodes.Status400BadRequest);
        }
        catch (Exception ex)
        {
            caughtException = ex;
        }

        // Assert
        caughtException.Should().NotBeNull();
        caughtException.Should().BeOfType<DomainException>();
    }

    [Fact]
    public void DomainException_WithEmptyMessage_IsAllowed()
    {
        // Arrange & Act
        var exception = new DomainException("");

        // Assert
        exception.Message.Should().BeEmpty();
        exception.StatusCode.Should().Be(StatusCodes.Status400BadRequest);
    }

    [Fact]
    public void DomainException_WithLongMessage_HandlesCorrectly()
    {
        // Arrange
        var longMessage = new string('a', 1000);

        // Act
        var exception = new DomainException(longMessage);

        // Assert
        exception.Message.Should().Be(longMessage);
    }
}
