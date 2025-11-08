using System.Security.Claims;

namespace ProjectService.Security;

public static class UserContext
{
    public static Guid? UserId(ClaimsPrincipal principal)
    {
        var raw = principal.FindFirstValue("sub") ?? principal.FindFirstValue(ClaimTypes.NameIdentifier);
        return Guid.TryParse(raw, out var id) ? id : null;
    }

    public static bool IsInRole(ClaimsPrincipal principal, string role) => principal.IsInRole(role);

    public static bool IsManager(ClaimsPrincipal principal) => IsInRole(principal, "manager");
    public static bool IsEmployee(ClaimsPrincipal principal) => IsInRole(principal, "employee");
    public static bool IsCustomer(ClaimsPrincipal principal) => IsInRole(principal, "customer");
}
