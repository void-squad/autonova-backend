CREATE USER project_user WITH PASSWORD 'project_pass';
CREATE DATABASE autonova OWNER project_user;
\connect autonova
CREATE SCHEMA IF NOT EXISTS project AUTHORIZATION project_user;
GRANT USAGE ON SCHEMA project TO project_user;
ALTER ROLE project_user SET search_path = project;
GRANT CREATE, USAGE ON SCHEMA project TO project_user;
