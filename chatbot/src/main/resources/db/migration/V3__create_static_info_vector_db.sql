-- Ensure required extensions are enabled (safe to run even if already enabled)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Table for static information and its embedding vector
CREATE TABLE IF NOT EXISTS static_info_vector_db (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    topic VARCHAR(255) NOT NULL,
    description TEXT,
    embedding vector(384)
);

-- Optional index for faster nearest-neighbor searches using ivfflat
CREATE INDEX IF NOT EXISTS idx_static_info_embedding
    ON static_info_vector_db
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
