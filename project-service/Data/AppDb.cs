using Microsoft.EntityFrameworkCore;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;

namespace ProjectService.Data;

public class AppDb(DbContextOptions<AppDb> options) : DbContext(options)
{
    public DbSet<Project> Projects => Set<Project>();
    public DbSet<ProjectTask> Tasks => Set<ProjectTask>();
    public DbSet<ProjectActivity> Activity => Set<ProjectActivity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.HasDefaultSchema("project");
        modelBuilder.HasPostgresExtension("btree_gin");

        modelBuilder.Entity<Project>(entity =>
        {
            entity.HasKey(e => e.ProjectId);
            entity.Property(e => e.Title)
                  .IsRequired()
                  .HasMaxLength(200);
            entity.Property(e => e.Description)
                  .HasMaxLength(4000);
            entity.Property(e => e.Status)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.CreatedAt).IsRequired();
            entity.Property(e => e.UpdatedAt).IsRequired();

            entity.HasMany(e => e.Tasks)
                  .WithOne(t => t.Project)
                  .HasForeignKey(t => t.ProjectId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Activity)
                  .WithOne(a => a.Project)
                  .HasForeignKey(a => a.ProjectId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => new { e.CustomerId, e.CreatedAt })
                  .HasDatabaseName("IX_Projects_CustomerId_CreatedAt")
                  .IsDescending(false, true);
        });

        modelBuilder.Entity<ProjectTask>(entity =>
        {
            entity.HasKey(e => e.TaskId);
            entity.Property(e => e.Title)
                  .IsRequired()
                  .HasMaxLength(200);
            entity.Property(e => e.ServiceType)
                  .IsRequired()
                  .HasMaxLength(160);
            entity.Property(e => e.Status)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.CreatedAt).IsRequired();
            entity.Property(e => e.UpdatedAt).IsRequired();

            entity.HasMany(e => e.Activity)
                  .WithOne(a => a.Task!)
                  .HasForeignKey(a => a.TaskId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => new { e.AssigneeId, e.Status });
        });

        modelBuilder.Entity<ProjectActivity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.ActorRole)
                  .HasMaxLength(50)
                  .IsRequired();
            entity.Property(e => e.Message)
                  .HasMaxLength(500)
                  .IsRequired();
            entity.Property(e => e.CreatedAt)
                  .IsRequired();

            entity.HasIndex(e => e.ProjectId);
            entity.HasIndex(e => e.TaskId);
        });
    }
}
