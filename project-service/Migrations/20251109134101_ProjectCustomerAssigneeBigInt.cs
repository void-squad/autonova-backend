using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class ProjectCustomerAssigneeBigInt : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects");

            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_assignee;");
            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_project_assignee;");

            migrationBuilder.DropColumn(
                name: "CustomerId",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropColumn(
                name: "AssigneeId",
                schema: "project",
                table: "Tasks");

            migrationBuilder.AddColumn<long>(
                name: "CustomerId",
                schema: "project",
                table: "Projects",
                type: "bigint",
                nullable: false,
                defaultValue: 0L);

            migrationBuilder.AddColumn<long>(
                name: "AssigneeId",
                schema: "project",
                table: "Tasks",
                type: "bigint",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects",
                columns: new[] { "CustomerId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_assignee ON project.""Tasks""(""AssigneeId"");");
            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_project_assignee ON project.""Tasks""(""ProjectId"", ""AssigneeId"");");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects");

            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_assignee;");
            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_project_assignee;");

            migrationBuilder.DropColumn(
                name: "CustomerId",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropColumn(
                name: "AssigneeId",
                schema: "project",
                table: "Tasks");

            migrationBuilder.AddColumn<Guid>(
                name: "CustomerId",
                schema: "project",
                table: "Projects",
                type: "uuid",
                nullable: false,
                defaultValue: Guid.Empty);

            migrationBuilder.AddColumn<Guid>(
                name: "AssigneeId",
                schema: "project",
                table: "Tasks",
                type: "uuid",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_Projects_CustomerId_CreatedAt",
                schema: "project",
                table: "Projects",
                columns: new[] { "CustomerId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_assignee ON project.""Tasks""(""AssigneeId"");");
            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_project_assignee ON project.""Tasks""(""ProjectId"", ""AssigneeId"");");
        }
    }
}
