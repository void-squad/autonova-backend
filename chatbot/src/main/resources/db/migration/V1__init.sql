-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Workflow Steps Table
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    step_name VARCHAR(255) NOT NULL,
    step_description TEXT,
    allowed_users TEXT[],
    required_local_data TEXT[],
    related_steps TEXT[],
    prompt_template TEXT,
    embedding vector(1536)
);

-- Optional: index for faster vector search
CREATE INDEX IF NOT EXISTS idx_workflow_steps_embedding
    ON workflow_steps
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
