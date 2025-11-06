-- Employee Dashboard Service Database Schema

-- Table: employee_preferences
CREATE TABLE IF NOT EXISTS employee_preferences (
    employee_id BIGINT PRIMARY KEY,
    default_view VARCHAR(20) NOT NULL CHECK (default_view IN ('OPERATIONAL', 'ANALYTICAL')),
    theme VARCHAR(10) NOT NULL CHECK (theme IN ('DARK', 'LIGHT')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table: saved_analytics_reports
CREATE TABLE IF NOT EXISTS saved_analytics_reports (
    report_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    report_parameters JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employee_preferences(employee_id) ON DELETE CASCADE
);

-- Create index on employee_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_saved_reports_employee_id ON saved_analytics_reports(employee_id);

-- Create index on report_name for searching
CREATE INDEX IF NOT EXISTS idx_saved_reports_name ON saved_analytics_reports(report_name);

-- Sample data for testing (optional)
-- INSERT INTO employee_preferences (employee_id, default_view, theme) VALUES 
--     (1, 'OPERATIONAL', 'LIGHT'),
--     (2, 'ANALYTICAL', 'DARK');
