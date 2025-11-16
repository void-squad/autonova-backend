CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS feedbacks (
    feedback_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    userid BIGINT,
    feedback TEXT,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT now()
);
