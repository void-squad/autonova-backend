using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;
using ProjectService.Data;

public class AppDbFactory : IDesignTimeDbContextFactory<AppDb>
{
    public AppDb CreateDbContext(string[] args)
    {
        var config = new ConfigurationBuilder()
            .AddJsonFile("appsettings.json", optional: false)
            .AddEnvironmentVariables()
            .Build();

        var cs = config.GetConnectionString("Postgres");
        var options = new DbContextOptionsBuilder<AppDb>()
            .UseNpgsql(cs, o =>
            {
                o.MigrationsHistoryTable("__EFMigrationsHistory", "project");
            })
            .Options;

        return new AppDb(options);
    }
}
