--liquibase formatted sql
--changeset krish:006-fix-avg-resolution-hours-type
ALTER TABLE analytics_snapshots
ALTER COLUMN avg_resolution_hours
TYPE DOUBLE PRECISION;
