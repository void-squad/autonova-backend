using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class ProjectNumericId : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<long>(
                name: "Id",
                schema: "project",
                table: "Projects",
                type: "bigint",
                nullable: false,
                defaultValue: 0L)
                .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn);

            migrationBuilder.Sql("""
                UPDATE project."Projects"
                SET "Id" = nextval(pg_get_serial_sequence('project."Projects"', 'Id'))
                WHERE "Id" = 0;
            """);

            migrationBuilder.Sql("""
                SELECT setval(
                    pg_get_serial_sequence('project."Projects"', 'Id'),
                    GREATEST(1, COALESCE((SELECT MAX("Id") FROM project."Projects"), 0))
                );
            """);

            migrationBuilder.CreateIndex(
                name: "IX_Projects_Id",
                schema: "project",
                table: "Projects",
                column: "Id",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Projects_Id",
                schema: "project",
                table: "Projects");

            migrationBuilder.DropColumn(
                name: "Id",
                schema: "project",
                table: "Projects");
        }
    }
}
