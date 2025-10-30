using Microsoft.EntityFrameworkCore;
using ProjectService.Domain.Entities;
using ProjectService.Domain.Enums;

namespace ProjectService.Data;

public class AppDb(DbContextOptions<AppDb> options) : DbContext(options)
{
    public DbSet<Project> Projects => Set<Project>();
    public DbSet<TaskItem> Tasks => Set<TaskItem>();
    public DbSet<Quote> Quotes => Set<Quote>();
    public DbSet<StatusHistory> StatusHistory => Set<StatusHistory>();
    public DbSet<OutboxMessage> Outbox => Set<OutboxMessage>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.HasDefaultSchema("project");

        modelBuilder.Entity<Project>(entity =>
        {
            entity.HasKey(e => e.ProjectId);
            entity.Property(e => e.Title)
                  .IsRequired()
                  .HasMaxLength(200);
            entity.Property(e => e.Status)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.CreatedAt).IsRequired();
            entity.Property(e => e.UpdatedAt).IsRequired();

            entity.HasMany(e => e.Tasks)
                  .WithOne(t => t.Project!)
                  .HasForeignKey(t => t.ProjectId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Quotes)
                  .WithOne(q => q.Project!)
                  .HasForeignKey(q => q.ProjectId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.StatusHistory)
                  .WithOne(h => h.Project!)
                  .HasForeignKey(h => h.ProjectId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<TaskItem>(entity =>
        {
            entity.HasKey(e => e.TaskId);
            entity.Property(e => e.Title)
                  .IsRequired()
                  .HasMaxLength(200);
            entity.Property(e => e.Status)
                  .IsRequired()
                  .HasMaxLength(80);
            entity.Property(e => e.EstimateHours)
                  .HasColumnType("numeric(10,2)");
        });

        modelBuilder.Entity<Quote>(entity =>
        {
            entity.HasKey(e => e.QuoteId);
            entity.Property(e => e.Status)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.Total)
                  .HasColumnType("numeric(12,2)");
            entity.Property(e => e.IssuedAt)
                  .IsRequired();
        });

        modelBuilder.Entity<StatusHistory>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.FromStatus)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.ToStatus)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.ChangedAt)
                  .IsRequired();
        });

        modelBuilder.Entity<OutboxMessage>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Topic)
                  .IsRequired()
                  .HasMaxLength(200);
            entity.Property(e => e.Payload)
                  .IsRequired();
            entity.Property(e => e.CreatedAt)
                  .IsRequired();
        });
    }
}
