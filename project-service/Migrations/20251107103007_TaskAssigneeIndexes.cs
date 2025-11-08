using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class TaskAssigneeIndexes : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_assignee ON project.""Tasks""(""AssigneeId"");");
            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_project_assignee ON project.""Tasks""(""ProjectId"", ""AssigneeId"");");
            migrationBuilder.Sql(@"CREATE INDEX IF NOT EXISTS ix_tasks_status ON project.""Tasks""(""Status"");");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_assignee;");
            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_project_assignee;");
            migrationBuilder.Sql(@"DROP INDEX IF EXISTS ix_tasks_status;");
        }
    }
}
