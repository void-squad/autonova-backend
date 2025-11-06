#!/bin/bash
set -e

echo "Installing pgvector..."
apt-get update
apt-get install -y postgresql-$(psql -V | awk '{print $3}' | cut -d. -f1)-pgvector
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE EXTENSION IF NOT EXISTS vector;"
echo "pgvector installed successfully."