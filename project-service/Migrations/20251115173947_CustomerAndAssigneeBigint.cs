using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ProjectService.Migrations
{
    /// <inheritdoc />
    public partial class CustomerAndAssigneeBigint : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                CREATE OR REPLACE FUNCTION project.uuid_to_int64(u uuid)
                RETURNS bigint
                LANGUAGE SQL
                IMMUTABLE
                AS $$
                    SELECT CASE
                        WHEN u IS NULL THEN NULL
                        ELSE hashtextextended(u::text, 0)
                    END;
                $$;
            """);

            migrationBuilder.Sql("""
                ALTER TABLE project."Projects"
                    ALTER COLUMN "CustomerId" TYPE bigint USING project.uuid_to_int64("CustomerId"),
                    ALTER COLUMN "CreatedBy" TYPE bigint USING project.uuid_to_int64("CreatedBy");
            """);

            migrationBuilder.Sql("""
                ALTER TABLE project."Tasks"
                    ALTER COLUMN "AssigneeId" TYPE bigint USING project.uuid_to_int64("AssigneeId");
            """);

            migrationBuilder.Sql("""
                ALTER TABLE project."Activity"
                    ALTER COLUMN "ActorId" TYPE bigint USING project.uuid_to_int64("ActorId");
            """);

            migrationBuilder.Sql("""DROP FUNCTION project.uuid_to_int64(uuid);""");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                CREATE OR REPLACE FUNCTION project.int64_to_uuid(v bigint)
                RETURNS uuid
                LANGUAGE SQL
                IMMUTABLE
                AS $$
                    SELECT CASE
                        WHEN v IS NULL THEN NULL
                        ELSE (
                            '00000000-0000-0000-'
                            || lpad(to_hex((v >> 48) & 65535), 4, '0')
                            || '-'
                            || lpad(to_hex(v & 281474976710655), 12, '0')
                        )::uuid
                    END;
                $$;
            """);

            migrationBuilder.Sql("""
                ALTER TABLE project."Projects"
                    ALTER COLUMN "CustomerId" TYPE uuid USING project.int64_to_uuid("CustomerId"),
                    ALTER COLUMN "CreatedBy" TYPE uuid USING project.int64_to_uuid("CreatedBy");
            """);

            migrationBuilder.Sql("""
                ALTER TABLE project."Tasks"
                    ALTER COLUMN "AssigneeId" TYPE uuid USING project.int64_to_uuid("AssigneeId");
            """);

            migrationBuilder.Sql("""
                ALTER TABLE project."Activity"
                    ALTER COLUMN "ActorId" TYPE uuid USING project.int64_to_uuid("ActorId");
            """);

            migrationBuilder.Sql("""DROP FUNCTION project.int64_to_uuid(bigint);""");
        }
    }
}
