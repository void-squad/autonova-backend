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
    public DbSet<ChangeRequest> ChangeRequests => Set<ChangeRequest>();
    public DbSet<OutboxMessage> Outbox => Set<OutboxMessage>();

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
            entity.Property(e => e.Status)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.CreatedAt).IsRequired();
            entity.Property(e => e.UpdatedAt).IsRequired();
            entity.Property(e => e.Budget)
                  .HasColumnType("numeric(14,2)")
                  .HasDefaultValue(0m);
            entity.Property(e => e.DueDate)
                  .HasColumnType("date");
            entity.Property(e => e.ClientRequestId)
                  .HasMaxLength(64);

            entity.UseXminAsConcurrencyToken();

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

            entity.HasMany(e => e.ChangeRequests)
                  .WithOne(cr => cr.Project)
                  .HasForeignKey(cr => cr.ProjectId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => new { e.CustomerId, e.CreatedAt })
                  .HasDatabaseName("IX_Projects_CustomerId_CreatedAt")
                  .IsDescending(false, true);

            entity.HasIndex(e => e.Status)
                  .HasDatabaseName("IX_Projects_Status");

            entity.HasIndex(e => e.ClientRequestId)
                  .HasDatabaseName("UX_Projects_ClientRequestId")
                  .IsUnique()
                  .HasFilter("\"ClientRequestId\" IS NOT NULL");
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
            entity.Property(e => e.ClientRequestId)
                  .HasMaxLength(64);

            entity.UseXminAsConcurrencyToken();

            entity.HasIndex(e => e.ClientRequestId)
                  .HasDatabaseName("UX_Quotes_ClientRequestId")
                  .IsUnique()
                  .HasFilter("\"ClientRequestId\" IS NOT NULL");
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
            entity.Property(e => e.Note)
                  .HasMaxLength(240);

            entity.HasIndex(e => e.ProjectId)
                  .HasDatabaseName("IX_StatusHistory_ProjectId");
        });

        modelBuilder.Entity<ChangeRequest>(entity =>
        {
            entity.HasKey(e => e.ChangeRequestId);
            entity.Property(e => e.Title)
                  .IsRequired()
                  .HasMaxLength(120);
            entity.Property(e => e.Description)
                  .HasMaxLength(4000);
            entity.Property(e => e.Status)
                  .HasConversion<string>()
                  .IsRequired();
            entity.Property(e => e.ProposedPriceDelta)
                  .HasColumnType("numeric(12,2)");
            entity.Property(e => e.ProposedNewDueDate)
                  .HasColumnType("date");
            entity.Property(e => e.ClientRequestId)
                  .HasMaxLength(64);

            entity.UseXminAsConcurrencyToken();

            entity.HasIndex(e => new { e.ProjectId, e.CreatedAt })
                  .HasDatabaseName("IX_ChangeRequests_ProjectId_CreatedAt")
                  .IsDescending(false, true);

            entity.HasIndex(e => new { e.ProjectId, e.ClientRequestId })
                  .HasDatabaseName("UX_ChangeRequests_ProjectId_ClientRequestId")
                  .IsUnique()
                  .HasFilter("\"ClientRequestId\" IS NOT NULL");

            entity.HasIndex(e => e.ClientRequestId)
                  .HasDatabaseName("UX_ChangeRequests_ClientRequestId")
                  .IsUnique()
                  .HasFilter("\"ClientRequestId\" IS NOT NULL");
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
