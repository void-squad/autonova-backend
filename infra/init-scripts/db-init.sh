#!/bin/bash
set -e

# ========================
# Configuration
# ========================
PGHOST=${PGHOST}
PGPORT=${PGPORT}
POSTGRES_USER=${POSTGRES_USER}
PGPASSWORD=${PGPASSWORD:-$POSTGRES_PASSWORD}  # needed for TCP authentication
# Optional: if using Neon, connect initially to the default DB they provide (usually `neondb`)
PGDATABASE=${PGDATABASE:-postgres}  # fallback to postgres if not set

export PGPASSWORD

echo "========================================="
echo "Starting database initialization..."
echo "========================================="

# ========================
# SSL detection (Neon vs local)
# ========================
if [[ "$PGHOST" == "localhost" || "$PGHOST" == "127.0.0.1" || "$PGHOST" == "postgres" ]]; then
  SSLMODE_OPTS=""
  echo "Local Postgres detected — no SSL."
else
  SSLMODE_OPTS="sslmode=require"
  echo "Remote Postgres (Neon) detected — using SSL."
fi

# ========================
# Define services
# Format: database:user:password_env_var
# ========================
SERVICES=(
    "projects_db:project_service:PROJECTS_DB_PASSWORD"
    "progress_monitoring_db:progress_monitoring_service:PROGRESS_MONITORING_DB_PASSWORD"
    "employee_dashboard_db:employee_dashboard_service:EMPLOYEE_DASHBOARD_DB_PASSWORD"
    "vector_db:chatbot_service:VECTOR_DB_PASSWORD"
    "notifications_db:notification_service:NOTIFICATION_DB_PASSWORD"
    "user_management_db:user_management_service:USER_MANAGEMENT_DB_PASSWORD"

    # Add dbs for serivices as needed
)

# ========================
# Loop through services
# ========================
for service in "${SERVICES[@]}"; do
    IFS=':' read -r db user pwd_var <<< "$service"
    password="${!pwd_var}"
    
    echo ""
    echo "----------------------------------------"
    echo "Processing: $db / $user"
    echo "----------------------------------------"
    
    # Check if database exists
    DB_EXISTS=$(psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 -tAc "SELECT 1 FROM pg_database WHERE datname='$db'")
    
    if [ "$DB_EXISTS" = "1" ]; then
        echo "→ Database already exists: $db"
    else
        echo "✓ Creating database: $db"
        psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 -c "CREATE DATABASE $db;"
    fi
    
    # Check if user exists
    USER_EXISTS=$(psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 -tAc "SELECT 1 FROM pg_roles WHERE rolname='$user'")
    
    if [ "$USER_EXISTS" = "1" ]; then
        echo "→ User already exists: $user"
    else
        echo "✓ Creating user: $user"
        psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 -c "CREATE USER $user WITH ENCRYPTED PASSWORD '$password';"
    fi
    
# Grant privileges and set ownership
echo "✓ Ensuring privileges for: $user"

if [[ "$PGHOST" == "localhost" || "$PGHOST" == "127.0.0.1" ]]; then
    # Local Postgres — full ownership allowed
    psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 <<EOSQL
GRANT ALL PRIVILEGES ON DATABASE $db TO $user;
ALTER DATABASE $db OWNER TO $user;

EOSQL
else
    # Neon — only grant privileges, skip changing owner, but grant schema permissions
    psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 <<EOSQL
GRANT ALL PRIVILEGES ON DATABASE $db TO $user;
EOSQL
    
    # Grant schema permissions in the specific database
    psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$db $SSLMODE_OPTS" -v ON_ERROR_STOP=1 <<EOSQL
GRANT ALL ON SCHEMA public TO $user;
GRANT CREATE ON SCHEMA public TO $user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $user;
EOSQL
fi

echo "✓ Completed: $db / $user"

done

# ========================
# Final summary
# ========================
echo ""
echo "========================================"
echo "Database initialization completed!"
echo "========================================"

psql "host=$PGHOST port=$PGPORT user=$POSTGRES_USER password=$PGPASSWORD dbname=$PGDATABASE $SSLMODE_OPTS" -v ON_ERROR_STOP=1 <<-EOSQL
    \echo ''
    \echo 'Listing all databases:'
    \l
    
    \echo ''
    \echo 'Listing all users/roles:'
    \du
    
    \echo ''
    \echo 'Database ownership summary:'
    SELECT datname, pg_catalog.pg_get_userbyid(datdba) as owner 
    FROM pg_database;
EOSQL

echo ""
echo "✓ All done!"
