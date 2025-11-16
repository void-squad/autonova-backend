using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class NormalizeProjectStatusActive : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                UPDATE project."Projects"
                SET "Status" = 'InProgress'
                WHERE "Status" = 'Active';
            """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                UPDATE project."Projects"
                SET "Status" = 'Active'
                WHERE "Status" = 'InProgress';
            """);
        }
    }
}
