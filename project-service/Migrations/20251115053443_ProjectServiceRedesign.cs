using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class ProjectServiceRedesign : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.EnsureSchema(
                name: "project");

            migrationBuilder.AlterDatabase()
                .Annotation("Npgsql:PostgresExtension:btree_gin", ",,");

            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"Activity\" CASCADE;");
            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"Tasks\" CASCADE;");
            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"StatusHistory\" CASCADE;");
            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"ChangeRequests\" CASCADE;");
            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"Quotes\" CASCADE;");
            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"Projects\" CASCADE;");
            migrationBuilder.Sql("DROP TABLE IF EXISTS project.\"Outbox\" CASCADE;");

            migrationBuilder.CreateTable(
                name: "Projects",
                schema: "project",
                columns: table => new
                {
                    ProjectId = table.Column<Guid>(type: "uuid", nullable: false),
                    CustomerId = table.Column<Guid>(type: "uuid", nullable: false),
                    VehicleId = table.Column<Guid>(type: "uuid", nullable: false),
                    Title = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    Description = table.Column<string>(type: "character varying(4000)", maxLength: 4000, nullable: true),
                    Status = table.Column<string>(type: "text", nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    RequestedStart = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    RequestedEnd = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    ApprovedStart = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    ApprovedEnd = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    CreatedBy = table.Column<Guid>(type: "uuid", nullable: false),
                    AppointmentId = table.Column<Guid>(type: "uuid", nullable: true),
                    AppointmentSnapshot = table.Column<string>(type: "text", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Projects", x => x.ProjectId);
                });

            migrationBuilder.CreateTable(
                name: "Tasks",
                schema: "project",
                columns: table => new
                {
                    TaskId = table.Column<Guid>(type: "uuid", nullable: false),
                    ProjectId = table.Column<Guid>(type: "uuid", nullable: false),
                    Title = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    ServiceType = table.Column<string>(type: "character varying(160)", maxLength: 160, nullable: false),
                    Detail = table.Column<string>(type: "text", nullable: true),
                    Status = table.Column<string>(type: "text", nullable: false),
                    AssigneeId = table.Column<Guid>(type: "uuid", nullable: true),
                    EstimateHours = table.Column<decimal>(type: "numeric", nullable: true),
                    ScheduledStart = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    ScheduledEnd = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    AppointmentId = table.Column<Guid>(type: "uuid", nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Tasks", x => x.TaskId);
                    table.ForeignKey(
                        name: "FK_Tasks_Projects_ProjectId",
                        column: x => x.ProjectId,
                        principalSchema: "project",
                        principalTable: "Projects",
                        principalColumn: "ProjectId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "Activity",
                schema: "project",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    ProjectId = table.Column<Guid>(type: "uuid", nullable: false),
                    TaskId = table.Column<Guid>(type: "uuid", nullable: true),
                    ActorId = table.Column<Guid>(type: "uuid", nullable: false),
                    ActorRole = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    Message = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Activity", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Activity_Projects_ProjectId",
                        column: x => x.ProjectId,
                        principalSchema: "project",
                        principalTable: "Projects",
                        principalColumn: "ProjectId",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_Activity_Tasks_TaskId",
                        column: x => x.TaskId,
                        principalSchema: "project",
                        principalTable: "Tasks",
                        principalColumn: "TaskId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_Activity_ProjectId",
                schema: "project",
                table: "Activity",
                column: "ProjectId");

            migrationBuilder.CreateIndex(
                name: "IX_Activity_TaskId",
                schema: "project",
                table: "Activity",
                column: "TaskId");

            migrationBuilder.CreateIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects",
                columns: new[] { "CustomerId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_Tasks_AssigneeId_Status",
                schema: "project",
                table: "Tasks",
                columns: new[] { "AssigneeId", "Status" });

            migrationBuilder.CreateIndex(
                name: "IX_Tasks_ProjectId",
                schema: "project",
                table: "Tasks",
                column: "ProjectId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "Activity",
                schema: "project");

            migrationBuilder.DropTable(
                name: "Tasks",
                schema: "project");

            migrationBuilder.DropTable(
                name: "Projects",
                schema: "project");
        }
    }
}
