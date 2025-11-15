using System.Linq;
using System.Security.Claims;

namespace ProjectService.Extensions;

public static class ClaimsPrincipalExtensions
{
    public static long GetUserId(this ClaimsPrincipal principal)
    {
        var claim = principal.FindFirst("sub") ?? principal.FindFirst(ClaimTypes.NameIdentifier);
        return claim is not null && long.TryParse(claim.Value, out var parsed)
            ? parsed
            : 0L;
    }

    public static string GetPrimaryRole(this ClaimsPrincipal principal)
    {
        static IEnumerable<string> EnumerateRoles(ClaimsPrincipal p)
        {
            foreach (var claim in p.FindAll("role"))
            {
                if (!string.IsNullOrWhiteSpace(claim.Value))
                {
                    yield return claim.Value;
                }
            }

            foreach (var claim in p.FindAll(ClaimTypes.Role))
            {
                if (!string.IsNullOrWhiteSpace(claim.Value))
                {
                    yield return claim.Value;
                }
            }
        }

        var roles = EnumerateRoles(principal)
            .Select(r => r.Trim().ToLowerInvariant())
            .Where(r => r.Length > 0)
            .ToList();

        if (roles.Contains("admin"))
        {
            return "admin";
        }

        if (roles.Contains("manager"))
        {
            return "manager";
        }

        if (roles.Contains("employee"))
        {
            return "employee";
        }

        if (roles.Contains("customer"))
        {
            return "customer";
        }

        return roles.FirstOrDefault() ?? "system";
    }
}
