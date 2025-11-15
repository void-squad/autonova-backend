using System.IO;
using System.Linq;
using System.Net.Mime;
using System.Text;
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
using ProjectService.Seeding;
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

var authSection = builder.Configuration.GetSection("Auth");

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        var authority = authSection["Authority"];
        if (!string.IsNullOrWhiteSpace(authority))
        {
            options.Authority = authority;
        }

        options.RequireHttpsMetadata = false;
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateAudience = authSection.GetValue("ValidateAudience", false),
            ValidateIssuer = authSection.GetValue("ValidateIssuer", false),
            RoleClaimType = "role",
            NameClaimType = authSection["NameClaimType"] ?? "email"
        };

        var signingKeyValue = authSection["SigningKey"];
        if (!string.IsNullOrWhiteSpace(signingKeyValue))
        {
            byte[] keyBytes;
            try
            {
                keyBytes = Convert.FromBase64String(signingKeyValue);
            }
            catch (FormatException)
            {
                keyBytes = Encoding.UTF8.GetBytes(signingKeyValue);
            }

            options.TokenValidationParameters.ValidateIssuerSigningKey = true;
            options.TokenValidationParameters.IssuerSigningKey = new SymmetricSecurityKey(keyBytes);
        }

        options.Events = new JwtBearerEvents
        {
            OnTokenValidated = context =>
            {
                var logger = context.HttpContext.RequestServices
                    .GetRequiredService<ILoggerFactory>()
                    .CreateLogger("JwtAuth");

                var role = context.Principal?.FindFirst("role")?.Value ?? "(none)";
                var subject = context.Principal?.Identity?.Name ?? "(anonymous)";
                logger.LogInformation("Token validated for {Subject} with role {Role}", subject, role);
                return Task.CompletedTask;
            },
            OnForbidden = context =>
            {
                var logger = context.HttpContext.RequestServices
                    .GetRequiredService<ILoggerFactory>()
                    .CreateLogger("JwtAuth");
                var role = context.Principal?.FindFirst("role")?.Value ?? "(none)";
                logger.LogWarning("Authorization failed for {Subject} with role {Role}", context.Principal?.Identity?.Name ?? "(anonymous)", role);
                return Task.CompletedTask;
            }
        };
    });

builder.Services.AddAuthorization(options =>
{
    options.FallbackPolicy = new AuthorizationPolicyBuilder()
        .RequireAuthenticatedUser()
        .Build();

    options.AddPolicy("AdminOnly", policy =>
        policy.RequireRole("ADMIN", "ROLE_ADMIN"));

    options.AddPolicy("EmployeeAccess", policy =>
        policy.RequireRole("ADMIN", "EMPLOYEE", "ROLE_ADMIN", "ROLE_EMPLOYEE"));

    options.AddPolicy("EmployeeOrAdmin", policy =>
        policy.RequireRole("EMPLOYEE", "ADMIN", "ROLE_EMPLOYEE", "ROLE_ADMIN"));

    options.AddPolicy("CustomerOnly", policy =>
        policy.RequireRole("CUSTOMER", "ROLE_CUSTOMER"));
});

builder.Services.AddCors(options =>
{
    var configuredOrigins = builder.Configuration.GetSection("Frontend:AllowedOrigins")
        .Get<string[]>() ?? Array.Empty<string>();

    var filteredOrigins = configuredOrigins
        .Where(o => !string.IsNullOrWhiteSpace(o))
        .Select(o => o.TrimEnd('/'))
        .ToArray();

    if (filteredOrigins.Length == 0)
    {
        var baseOrigin = builder.Configuration["Frontend:BaseUrl"] ?? "http://localhost:5173";
        filteredOrigins = new[] { baseOrigin.TrimEnd('/') };
    }

    options.AddPolicy("Frontend", policy =>
    {
        policy.WithOrigins(filteredOrigins)
            .AllowAnyHeader()
            .AllowAnyMethod()
            .AllowCredentials();
    });
});

builder.Services.AddScoped<DemoDataSeeder>();
builder.Services.AddValidatorsFromAssemblyContaining<CreateProjectRequestValidator>();
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

app.UseCors("Frontend");

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
