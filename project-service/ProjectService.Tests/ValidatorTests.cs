using FluentAssertions;
using ProjectService.Dtos;
using ProjectService.Validators;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xunit;

namespace ProjectService.Tests;

public class ValidatorTests
{
    [Fact]
    public async Task CreateProjectRequestValidator_WithValidData_Passes()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = "Test Project",
            Description = "Test Description",
            Tasks = new List<CreateProjectTaskRequest>()
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithEmptyVehicleId_Fails()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.Empty,
            Title = "Test Project"
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "VehicleId");
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithEmptyTitle_Fails()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = ""
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Title");
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithTitleTooLong_Fails()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = new string('a', 201) // 201 characters
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Title");
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithDescriptionTooLong_Fails()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            Description = new string('a', 4001) // 4001 characters
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Description");
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithNullDescription_Passes()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            Description = null
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithValidTasks_Passes()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            Tasks = new List<CreateProjectTaskRequest>
            {
                new CreateProjectTaskRequest
                {
                    Title = "Task 1",
                    ServiceType = "Repair"
                },
                new CreateProjectTaskRequest
                {
                    Title = "Task 2",
                    ServiceType = "Maintenance"
                }
            }
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task CreateProjectRequestValidator_WithInvalidTask_Fails()
    {
        // Arrange
        var validator = new CreateProjectRequestValidator();
        var request = new CreateProjectRequest
        {
            VehicleId = Guid.NewGuid(),
            Title = "Test",
            Tasks = new List<CreateProjectTaskRequest>
            {
                new CreateProjectTaskRequest
                {
                    Title = "", // Invalid
                    ServiceType = "Repair"
                }
            }
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName.Contains("Tasks"));
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithValidData_Passes()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = "Oil Change",
            ServiceType = "Maintenance",
            Detail = "Replace oil and filter"
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithEmptyTitle_Fails()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = "",
            ServiceType = "Maintenance"
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Title");
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithTitleTooLong_Fails()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = new string('a', 201),
            ServiceType = "Maintenance"
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Title");
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithEmptyServiceType_Fails()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = "Task",
            ServiceType = ""
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "ServiceType");
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithServiceTypeTooLong_Fails()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = "Task",
            ServiceType = new string('a', 161)
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "ServiceType");
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithDetailTooLong_Fails()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = "Task",
            ServiceType = "Test",
            Detail = new string('a', 2001)
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Detail");
    }

    [Fact]
    public async Task CreateProjectTaskRequestValidator_WithNullDetail_Passes()
    {
        // Arrange
        var validator = new CreateProjectTaskRequestValidator();
        var request = new CreateProjectTaskRequest
        {
            Title = "Task",
            ServiceType = "Test",
            Detail = null
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task ApproveProjectRequestValidator_WithValidData_Passes()
    {
        // Arrange
        var validator = new ApproveProjectRequestValidator();
        var request = new ApproveProjectRequest
        {
            Tasks = new List<ApproveProjectTaskUpdate>
            {
                new ApproveProjectTaskUpdate
                {
                    TaskId = Guid.NewGuid()
                }
            }
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task ApproveProjectRequestValidator_WithEmptyTaskId_Fails()
    {
        // Arrange
        var validator = new ApproveProjectRequestValidator();
        var request = new ApproveProjectRequest
        {
            Tasks = new List<ApproveProjectTaskUpdate>
            {
                new ApproveProjectTaskUpdate
                {
                    TaskId = Guid.Empty
                }
            }
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName.Contains("Tasks"));
    }

    [Fact]
    public async Task ApproveProjectRequestValidator_WithEmptyList_Passes()
    {
        // Arrange
        var validator = new ApproveProjectRequestValidator();
        var request = new ApproveProjectRequest
        {
            Tasks = new List<ApproveProjectTaskUpdate>()
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task UpdateTaskStatusRequestValidator_WithValidData_Passes()
    {
        // Arrange
        var validator = new UpdateTaskStatusRequestValidator();
        var request = new UpdateTaskStatusRequest
        {
            Status = Domain.Enums.TaskStatus.Completed,
            Note = "Task completed successfully"
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task UpdateTaskStatusRequestValidator_WithNullNote_Passes()
    {
        // Arrange
        var validator = new UpdateTaskStatusRequestValidator();
        var request = new UpdateTaskStatusRequest
        {
            Status = Domain.Enums.TaskStatus.InProgress,
            Note = null
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeTrue();
    }

    [Fact]
    public async Task UpdateTaskStatusRequestValidator_WithNoteTooLong_Fails()
    {
        // Arrange
        var validator = new UpdateTaskStatusRequestValidator();
        var request = new UpdateTaskStatusRequest
        {
            Status = Domain.Enums.TaskStatus.InProgress,
            Note = new string('a', 1001)
        };

        // Act
        var result = await validator.ValidateAsync(request);

        // Assert
        result.IsValid.Should().BeFalse();
        result.Errors.Should().Contain(e => e.PropertyName == "Note");
    }
}
