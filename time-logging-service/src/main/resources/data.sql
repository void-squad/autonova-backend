-- Insert Users
INSERT INTO users (id, email, first_name, last_name, role, created_at, updated_at) VALUES
('emp-001', 'john.doe@autoservice.com', 'John', 'Doe', 'EMPLOYEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('emp-002', 'jane.smith@autoservice.com', 'Jane', 'Smith', 'EMPLOYEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cust-001', 'alice.customer@email.com', 'Alice', 'Johnson', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cust-002', 'bob.customer@email.com', 'Bob', 'Williams', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Employees
INSERT INTO employees (user_id, employee_code, department, position, hourly_rate, hire_date, created_at) VALUES
('emp-001', 'EMP001', 'Service Department', 'Senior Mechanic', 35.50, '2023-01-15', CURRENT_TIMESTAMP),
('emp-002', 'EMP002', 'Modification Department', 'Automotive Technician', 32.00, '2023-03-20', CURRENT_TIMESTAMP);

-- Insert Customers
INSERT INTO customers (user_id, phone, address, created_at) VALUES
('cust-001', '+94 3481234567', '123 Main St, Springfield', CURRENT_TIMESTAMP),
('cust-002', '+94 9876543210', '456 Oak Ave, Riverside', CURRENT_TIMESTAMP);

-- Insert Vehicles
INSERT INTO vehicles (id, customer_id, make, model, year, license_plate, vin, color, created_at) VALUES
('veh-001', 'cust-001', 'Toyota', 'Camry', 2020, 'ABC-1234', '1HGBH41JXMN109186', 'Silver', CURRENT_TIMESTAMP),
('veh-002', 'cust-001', 'Honda', 'Accord', 2019, 'XYZ-5678', '2HGBH41JXMN109187', 'Blue', CURRENT_TIMESTAMP),
('veh-003', 'cust-002', 'Ford', 'F-150', 2021, 'DEF-9012', '3HGBH41JXMN109188', 'Black', CURRENT_TIMESTAMP);

-- Insert Projects
INSERT INTO projects (id, customer_id, vehicle_id, project_type, title, description, status, priority, estimated_cost, start_date, end_date, created_at, updated_at) VALUES
('proj-001', 'cust-001', 'veh-001', 'SERVICE', 'Regular Maintenance - Toyota Camry', 'Oil change, brake inspection, tire rotation', 'IN_PROGRESS', 'MEDIUM', 350.00, '2025-10-28', '2025-11-28', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('proj-002', 'cust-001', 'veh-002', 'MODIFICATION', 'Custom Exhaust System', 'Install performance exhaust system', 'IN_PROGRESS', 'HIGH', 1200.00, '2025-10-25', '2025-11-25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('proj-003', 'cust-002', 'veh-003', 'SERVICE', 'Engine Diagnostic', 'Check engine light diagnostic and repair', 'PENDING', 'HIGH', 500.00, '2025-10-29', '2025-11-29', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('proj-004', 'cust-001', 'veh-001', 'SERVICE', 'Brake Replacement', 'Replace front and rear brake pads', 'COMPLETED', 'MEDIUM', 450.00, '2025-10-15', '2025-11-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Project Tasks
INSERT INTO project_tasks (id, project_id, task_name, description, assigned_employee_id, estimated_hours, actual_hours, status, priority, due_date, created_at, updated_at) VALUES
-- Project 1 tasks (assigned to John - emp-001)
('task-001', 'proj-001', 'Oil Change', 'Replace engine oil and filter', 'emp-001', 1.0, 0.0, 'IN_PROGRESS', 'MEDIUM', CURRENT_DATE + INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-002', 'proj-001', 'Brake Inspection', 'Inspect brake pads and rotors', 'emp-001', 1.5, 0.0, 'TODO', 'MEDIUM', '2025-10-28', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-003', 'proj-001', 'Tire Rotation', 'Rotate all four tires', 'emp-001', 0.5, 0.0, 'TODO', 'LOW', CURRENT_DATE + INTERVAL '2 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Project 2 tasks (assigned to Jane - emp-002)
('task-004', 'proj-002', 'Remove Old Exhaust', 'Remove existing exhaust system', 'emp-002', 2.0, 1.5, 'COMPLETED', 'HIGH', CURRENT_DATE + INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-005', 'proj-002', 'Install New Exhaust', 'Install performance exhaust system', 'emp-002', 3.0, 0.0, 'IN_PROGRESS', 'HIGH', CURRENT_DATE + INTERVAL '2 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-006', 'proj-002', 'Test and Tune', 'Test exhaust sound and performance', 'emp-002', 1.0, 0.0, 'TODO', 'MEDIUM', CURRENT_DATE + INTERVAL '3 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Project 3 tasks (assigned to John - emp-001)
('task-007', 'proj-003', 'OBD Scan', 'Perform OBD-II diagnostic scan', 'emp-001', 0.5, 0.0, 'TODO', 'HIGH', CURRENT_DATE + INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-008', 'proj-003', 'Identify Issue', 'Diagnose root cause of check engine light', 'emp-001', 1.5, 0.0, 'TODO', 'HIGH', CURRENT_DATE + INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Project 4 tasks (completed project)
('task-009', 'proj-004', 'Front Brake Replacement', 'Replace front brake pads', 'emp-001', 2.0, 2.0, 'COMPLETED', 'MEDIUM', CURRENT_DATE + INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-010', 'proj-004', 'Rear Brake Replacement', 'Replace rear brake pads', 'emp-001', 1.5, 1.5, 'COMPLETED', 'MEDIUM', CURRENT_DATE + INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Time Logs (historical data for testing)
INSERT INTO time_logs (id, project_id, task_id, employee_id, hours, note, logged_at) VALUES
('log-001', 'proj-002', 'task-004', 'emp-002', 1.5, 'Removed old exhaust system, all bolts removed successfully', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
('log-002', 'proj-004', 'task-009', 'emp-001', 2.0, 'Replaced front brake pads, cleaned rotors', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
('log-003', 'proj-004', 'task-010', 'emp-001', 1.5, 'Replaced rear brake pads, tested brake response', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
('log-004', 'proj-001', 'task-001', 'emp-001', 0.75, 'Started oil change, drained old oil', CURRENT_TIMESTAMP - INTERVAL '15 minutes');