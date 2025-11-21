using System.Security.Claims;
using FluentAssertions;
using ProjectService.Extensions;
using Xunit;

namespace ProjectService.Tests;

public class ClaimsPrincipalExtensionsTests
{
    [Fact]
    public void GetUserId_WithSubClaim_ReturnsUserId()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("sub", "12345")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var userId = principal.GetUserId();

        // Assert
        userId.Should().Be(12345);
    }

    [Fact]
    public void GetUserId_WithNameIdentifierClaim_ReturnsUserId()
    {
        // Arrange
        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, "67890")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var userId = principal.GetUserId();

        // Assert
        userId.Should().Be(67890);
    }

    [Fact]
    public void GetUserId_WithBothClaims_PrefersSubClaim()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("sub", "111"),
            new Claim(ClaimTypes.NameIdentifier, "222")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var userId = principal.GetUserId();

        // Assert
        userId.Should().Be(111);
    }

    [Fact]
    public void GetUserId_WithNoClaim_ReturnsZero()
    {
        // Arrange
        var principal = new ClaimsPrincipal(new ClaimsIdentity());

        // Act
        var userId = principal.GetUserId();

        // Assert
        userId.Should().Be(0);
    }

    [Fact]
    public void GetUserId_WithInvalidFormat_ReturnsZero()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("sub", "not-a-number")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var userId = principal.GetUserId();

        // Assert
        userId.Should().Be(0);
    }

    [Fact]
    public void GetUserId_WithEmptyValue_ReturnsZero()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("sub", "")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var userId = principal.GetUserId();

        // Assert
        userId.Should().Be(0);
    }

    [Fact]
    public void GetPrimaryRole_WithAdminRole_ReturnsAdmin()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "admin"),
            new Claim("role", "employee")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("admin");
    }

    [Fact]
    public void GetPrimaryRole_WithManagerRole_ReturnsManager()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "manager"),
            new Claim("role", "employee")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("manager");
    }

    [Fact]
    public void GetPrimaryRole_WithEmployeeRole_ReturnsEmployee()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "employee"),
            new Claim("role", "customer")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("employee");
    }

    [Fact]
    public void GetPrimaryRole_WithCustomerRole_ReturnsCustomer()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "customer")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("customer");
    }

    [Fact]
    public void GetPrimaryRole_WithMixedCaseRoles_ReturnsNormalizedRole()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "ADMIN")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("admin");
    }

    [Fact]
    public void GetPrimaryRole_WithClaimTypesRole_ReturnsRole()
    {
        // Arrange
        var claims = new[]
        {
            new Claim(ClaimTypes.Role, "admin")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("admin");
    }

    [Fact]
    public void GetPrimaryRole_WithMixedClaimTypes_PrioritizesCorrectly()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "customer"),
            new Claim(ClaimTypes.Role, "admin")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("admin");
    }

    [Fact]
    public void GetPrimaryRole_WithNoRoles_ReturnsSystem()
    {
        // Arrange
        var principal = new ClaimsPrincipal(new ClaimsIdentity());

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("system");
    }

    [Fact]
    public void GetPrimaryRole_WithUnknownRole_ReturnsFirstRole()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "unknown-role")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("unknown-role");
    }

    [Fact]
    public void GetPrimaryRole_WithEmptyRoleValue_ReturnsSystem()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", ""),
            new Claim("role", "   ")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("system");
    }

    [Fact]
    public void GetPrimaryRole_WithWhitespaceInRole_TrimsCorrectly()
    {
        // Arrange
        var claims = new[]
        {
            new Claim("role", "  admin  ")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("admin");
    }

    [Fact]
    public void GetPrimaryRole_RespectsRolePriority()
    {
        // Arrange - admin has highest priority
        var claims = new[]
        {
            new Claim("role", "customer"),
            new Claim("role", "employee"),
            new Claim("role", "manager"),
            new Claim("role", "admin")
        };
        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims));

        // Act
        var role = principal.GetPrimaryRole();

        // Assert
        role.Should().Be("admin");
    }
}
