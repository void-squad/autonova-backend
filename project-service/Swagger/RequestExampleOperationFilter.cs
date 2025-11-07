using Microsoft.OpenApi.Any;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace ProjectService.Swagger;

public class RequestExampleOperationFilter : IOperationFilter
{
    public void Apply(OpenApiOperation operation, OperationFilterContext context)
    {
        if (operation?.RequestBody?.Content is null)
        {
            return;
        }

        var path = context.ApiDescription.RelativePath ?? string.Empty;
        var method = context.ApiDescription.HttpMethod?.ToUpperInvariant();
        if (string.IsNullOrEmpty(method))
        {
            return;
        }

        if (path.Equals("api/projects", StringComparison.OrdinalIgnoreCase) && method == "POST")
        {
            SetExample(operation.RequestBody, new OpenApiObject
            {
                ["customerId"] = new OpenApiString(Guid.NewGuid().ToString()),
                ["title"] = new OpenApiString("EV Fleet Modernization")
            });
        }
        else if (path.StartsWith("api/projects/{projectId}/change-requests", StringComparison.OrdinalIgnoreCase) && method == "POST")
        {
            SetExample(operation.RequestBody, new OpenApiObject
            {
                ["title"] = new OpenApiString("Extend scope to cover mobile app"),
                ["description"] = new OpenApiString("Add native capabilities requested by the client."),
                ["proposedPriceDelta"] = new OpenApiDouble(12500),
                ["proposedExtraHours"] = new OpenApiInteger(120),
                ["proposedNewDueDate"] = new OpenApiString(DateOnly.FromDateTime(DateTime.UtcNow.AddDays(30)).ToString("yyyy-MM-dd"))
            });
        }
        else if (path.StartsWith("api/change-requests/{changeRequestId}/approve", StringComparison.OrdinalIgnoreCase) && method == "POST")
        {
            SetExample(operation.RequestBody, new OpenApiObject
            {
                ["rowVersion"] = new OpenApiInteger(1)
            });
        }
    }

    private static void SetExample(OpenApiRequestBody requestBody, OpenApiObject example)
    {
        if (requestBody.Content.TryGetValue("application/json", out var mediaType))
        {
            mediaType.Example = example;
        }
    }
}
