-- Used ONLY by the "reset" profile (see application-reset.properties).
-- Drops every table so schema.sql can create them again and data.sql can load the demo data.
-- Children are dropped before parents, because a foreign key cannot point at a table that no longer exists.

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS join_requests;
DROP TABLE IF EXISTS team_seekers;
DROP TABLE IF EXISTS team_members;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS hackathons;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS colleges;
