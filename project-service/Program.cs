using System.IO;
using System.Linq;
using System.Net.Mime;
using System.Text.Json;
using System.Text.Json.Serialization;
using FluentValidation;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using Npgsql;
using ProjectService.Data;
using ProjectService.Dtos;
using ProjectService.HealthChecks;
using ProjectService.Messaging;
using ProjectService.Seeding;
using ProjectService.Services;
using ProjectService.Swagger;
using ProjectService.Validators;
using Steeltoe.Discovery.Eureka;

LoadDotEnv();

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddProblemDetails();
builder.Services.AddControllers().AddJsonOptions(options =>
{
    options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
});
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Project Service API",
        Version = "v1"
    });
    options.OperationFilter<RequestExampleOperationFilter>();
    options.MapType<DateOnly>(() => new OpenApiSchema { Type = "string", Format = "date" });
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Provide the JWT token prefixed with Bearer."
    });
    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

var connectionString = builder.Configuration.GetConnectionString("Postgres")
    ?? throw new InvalidOperationException("Postgres connection string is not configured.");

builder.Services.AddDbContext<AppDb>(options =>
{
    options.UseNpgsql(connectionString, o =>
    {
        o.MigrationsHistoryTable("__EFMigrationsHistory", "project");
    });
});

builder.Services.AddHttpClient("healthchecks")
    .ConfigureHttpClient(c => c.Timeout = TimeSpan.FromSeconds(5));

var healthChecksBuilder = builder.Services.AddHealthChecks();
healthChecksBuilder.AddCheck("postgres", () =>
{
    try
    {
        using var connection = new NpgsqlConnection(connectionString);
        connection.Open();
        return HealthCheckResult.Healthy();
    }
    catch (Exception ex)
    {
        return HealthCheckResult.Unhealthy(ex.Message);
    }
}, tags: new[] { "ready" });

var customersUrl = builder.Configuration["HealthChecks:CustomersUrl"];
if (!string.IsNullOrWhiteSpace(customersUrl))
{
    healthChecksBuilder.Add(new HealthCheckRegistration(
        "customers-api",
        sp => new UrlHealthCheck(customersUrl!, sp.GetRequiredService<IHttpClientFactory>()),
        HealthStatus.Degraded,
        new[] { "ready" }));
}

var appointmentsUrl = builder.Configuration["HealthChecks:AppointmentsUrl"];
if (!string.IsNullOrWhiteSpace(appointmentsUrl))
{
    healthChecksBuilder.Add(new HealthCheckRegistration(
        "appointments-api",
        sp => new UrlHealthCheck(appointmentsUrl!, sp.GetRequiredService<IHttpClientFactory>()),
        HealthStatus.Degraded,
        new[] { "ready" }));
}

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.RequireHttpsMetadata = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateAudience = builder.Configuration.GetValue("Auth:ValidateAudience", false),
            ValidateIssuer = true,
            RoleClaimType = "role"
        };
    });

builder.Services.AddAuthorization(options =>
{
    options.FallbackPolicy = new AuthorizationPolicyBuilder()
        .RequireAuthenticatedUser()
        .Build();

    options.AddPolicy("EmployeeOrManager", policy =>
        policy.RequireRole("employee", "manager"));

    options.AddPolicy("Customer", policy =>
        policy.RequireRole("customer"));

    options.AddPolicy("Manager", policy =>
        policy.RequireRole("manager"));
});

builder.Services.Configure<RabbitOptions>(builder.Configuration.GetSection("Rabbit"));
builder.Services.AddScoped<IProjectWorkflowService, ProjectWorkflowService>();
builder.Services.AddScoped<DemoDataSeeder>();
builder.Services.AddValidatorsFromAssemblyContaining<CreateProjectRequestValidator>();
builder.Services.AddHostedService<OutboxDispatcher>();
builder.Services.AddEurekaDiscoveryClient();

var app = builder.Build();

app.Logger.LogInformation("Applying database migrations...");

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDb>();
    await db.Database.MigrateAsync();
    app.Logger.LogInformation("Database migrations applied successfully.");

    var seeder = scope.ServiceProvider.GetRequiredService<DemoDataSeeder>();
    await seeder.SeedAsync();
}

app.UseExceptionHandler();

app.UseSwagger();
app.UseSwaggerUI();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();
app.MapHealthChecks("/healthz", new HealthCheckOptions
{
    Predicate = _ => false
}).AllowAnonymous();

app.MapHealthChecks("/readyz", new HealthCheckOptions
{
    Predicate = _ => true,
    ResultStatusCodes =
    {
        [HealthStatus.Healthy] = StatusCodes.Status200OK,
        [HealthStatus.Degraded] = StatusCodes.Status503ServiceUnavailable,
        [HealthStatus.Unhealthy] = StatusCodes.Status503ServiceUnavailable
    },
    ResponseWriter = WriteHealthReportAsync
}).AllowAnonymous();

app.Run();

static async Task WriteHealthReportAsync(HttpContext context, HealthReport report)
{
    context.Response.ContentType = MediaTypeNames.Application.Json;
    var payload = new
    {
        status = report.Status.ToString(),
        totalDuration = report.TotalDuration,
        results = report.Entries.Select(entry => new
        {
            name = entry.Key,
            status = entry.Value.Status.ToString(),
            duration = entry.Value.Duration,
            error = entry.Value.Exception?.Message
        })
    };

    await context.Response.WriteAsync(JsonSerializer.Serialize(payload));
}

static void LoadDotEnv()
{
    var possiblePaths = new[]
    {
        Path.Combine(Directory.GetCurrentDirectory(), ".env"),
        Path.Combine(AppContext.BaseDirectory, ".env")
    };

    var envFile = possiblePaths.FirstOrDefault(File.Exists);
    if (envFile is null)
    {
        return;
    }

    foreach (var rawLine in File.ReadAllLines(envFile))
    {
        var line = rawLine.Trim();
        if (string.IsNullOrEmpty(line) || line.StartsWith("#", StringComparison.Ordinal))
        {
            continue;
        }

        var parts = line.Split('=', 2);
        if (parts.Length != 2)
        {
            continue;
        }

        var key = parts[0].Trim();
        var value = parts[1].Trim().Trim('"');
        if (!string.IsNullOrEmpty(key))
        {
            Environment.SetEnvironmentVariable(key, value);
        }
    }
}
