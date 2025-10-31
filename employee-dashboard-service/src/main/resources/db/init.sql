-- Employee Dashboard Service Database Initialization Script

-- Create database (run as postgres superuser)
CREATE DATABASE employee_dashboard_db;

-- Connect to the database
\c employee_dashboard_db;

-- Create jobs table (will be created automatically by JPA, but this is for reference)
-- The table will be created automatically when the application starts
-- due to spring.jpa.hibernate.ddl-auto=update in application.properties

-- Sample data for testing (optional)
-- INSERT INTO jobs (id, title, description, project_id, employee_id, status, estimated_hours, created_at, updated_at)
-- VALUES 
--   (gen_random_uuid(), 'Fix brake system', 'Replace brake pads and rotors', gen_random_uuid(), gen_random_uuid(), 'PENDING', 4, NOW(), NOW()),
--   (gen_random_uuid(), 'Engine oil change', 'Full synthetic oil change', gen_random_uuid(), gen_random_uuid(), 'ACTIVE', 1, NOW(), NOW()),
--   (gen_random_uuid(), 'Tire rotation', 'Rotate all four tires', gen_random_uuid(), gen_random_uuid(), 'PENDING', 1, NOW(), NOW());
