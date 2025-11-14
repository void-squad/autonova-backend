using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class ChangeRequestsAndHardening : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AlterDatabase()
                .Annotation("Npgsql:PostgresExtension:btree_gin", ",,");

            migrationBuilder.AddColumn<string>(
                name: "Note",
                schema: "project",
                table: "StatusHistory",
                type: "character varying(240)",
                maxLength: 240,
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "ApprovedBy",
                schema: "project",
                table: "Quotes",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "ClientRequestId",
                schema: "project",
                table: "Quotes",
                type: "character varying(64)",
                maxLength: 64,
                nullable: true);

            migrationBuilder.AddColumn<DateTimeOffset>(
                name: "RejectedAt",
                schema: "project",
                table: "Quotes",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "RejectedBy",
                schema: "project",
                table: "Quotes",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<uint>(
                name: "xmin",
                schema: "project",
                table: "Quotes",
                type: "xid",
                rowVersion: true,
                nullable: false,
                defaultValue: 0u);

            migrationBuilder.AddColumn<decimal>(
                name: "Budget",
                schema: "project",
                table: "Projects",
                type: "numeric(14,2)",
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.AddColumn<string>(
                name: "ClientRequestId",
                schema: "project",
                table: "Projects",
                type: "character varying(64)",
                maxLength: 64,
                nullable: true);

            migrationBuilder.AddColumn<DateOnly>(
                name: "DueDate",
                schema: "project",
                table: "Projects",
                type: "date",
                nullable: true);

            migrationBuilder.AddColumn<uint>(
                name: "xmin",
                schema: "project",
                table: "Projects",
                type: "xid",
                rowVersion: true,
                nullable: false,
                defaultValue: 0u);

            migrationBuilder.CreateTable(
                name: "ChangeRequests",
                schema: "project",
                columns: table => new
                {
                    ChangeRequestId = table.Column<Guid>(type: "uuid", nullable: false),
                    ProjectId = table.Column<Guid>(type: "uuid", nullable: false),
                    Title = table.Column<string>(type: "character varying(120)", maxLength: 120, nullable: false),
                    Description = table.Column<string>(type: "character varying(4000)", maxLength: 4000, nullable: true),
                    ProposedPriceDelta = table.Column<decimal>(type: "numeric(12,2)", nullable: true),
                    ProposedExtraHours = table.Column<int>(type: "integer", nullable: true),
                    ProposedNewDueDate = table.Column<DateOnly>(type: "date", nullable: true),
                    Status = table.Column<string>(type: "text", nullable: false),
                    CreatedBy = table.Column<Guid>(type: "uuid", nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    DecidedBy = table.Column<Guid>(type: "uuid", nullable: true),
                    DecidedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    ClientRequestId = table.Column<string>(type: "character varying(64)", maxLength: 64, nullable: true),
                    xmin = table.Column<uint>(type: "xid", rowVersion: true, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ChangeRequests", x => x.ChangeRequestId);
                    table.ForeignKey(
                        name: "FK_ChangeRequests_Projects_ProjectId",
                        column: x => x.ProjectId,
                        principalSchema: "project",
                        principalTable: "Projects",
                        principalColumn: "ProjectId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "UX_Quotes_ClientRequestId",
                schema: "project",
                table: "Quotes",
                column: "ClientRequestId",
                unique: true,
                filter: "\"ClientRequestId\" IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects",
                columns: new[] { "CustomerId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_Projects_Status",
                schema: "project",
                table: "Projects",
                column: "Status");

            migrationBuilder.CreateIndex(
                name: "UX_Projects_ClientRequestId",
                schema: "project",
                table: "Projects",
                column: "ClientRequestId",
                unique: true,
                filter: "\"ClientRequestId\" IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "IX_ChangeRequests_ProjectId_CreatedAt",
                schema: "project",
                table: "ChangeRequests",
                columns: new[] { "ProjectId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "UX_ChangeRequests_ClientRequestId",
                schema: "project",
                table: "ChangeRequests",
                column: "ClientRequestId",
                unique: true,
                filter: "\"ClientRequestId\" IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "UX_ChangeRequests_ProjectId_ClientRequestId",
                schema: "project",
                table: "ChangeRequests",
                columns: new[] { "ProjectId", "ClientRequestId" },
                unique: true,
                filter: "\"ClientRequestId\" IS NOT NULL");

            migrationBuilder.Sql(
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_quotes_project_approved ON project.\"Quotes\"(\"ProjectId\") WHERE \"Status\" = 'Approved';");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "ChangeRequests",
                schema: "project");

            migrationBuilder.DropIndex(
                name: "UX_Quotes_ClientRequestId",
                schema: "project",
                table: "Quotes");

            migrationBuilder.DropIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropIndex(
                name: "IX_Projects_Status",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropIndex(
                name: "UX_Projects_ClientRequestId",
                schema: "project",
                table: "Projects");

            migrationBuilder.Sql("DROP INDEX IF EXISTS ux_quotes_project_approved;");

            migrationBuilder.DropColumn(
                name: "Note",
                schema: "project",
                table: "StatusHistory");

            migrationBuilder.DropColumn(
                name: "ApprovedBy",
                schema: "project",
                table: "Quotes");

            migrationBuilder.DropColumn(
                name: "ClientRequestId",
                schema: "project",
                table: "Quotes");

            migrationBuilder.DropColumn(
                name: "RejectedAt",
                schema: "project",
                table: "Quotes");

            migrationBuilder.DropColumn(
                name: "RejectedBy",
                schema: "project",
                table: "Quotes");

            migrationBuilder.DropColumn(
                name: "xmin",
                schema: "project",
                table: "Quotes");

            migrationBuilder.DropColumn(
                name: "Budget",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropColumn(
                name: "ClientRequestId",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropColumn(
                name: "DueDate",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropColumn(
                name: "xmin",
                schema: "project",
                table: "Projects");

            migrationBuilder.AlterDatabase()
                .OldAnnotation("Npgsql:PostgresExtension:btree_gin", ",,");
        }
    }
}
