ALTER TABLE workflow_steps
DROP COLUMN IF EXISTS required_local_data,
DROP COLUMN IF EXISTS related_steps;

ALTER TABLE workflow_steps
    RENAME COLUMN prompt_template TO systemContext;