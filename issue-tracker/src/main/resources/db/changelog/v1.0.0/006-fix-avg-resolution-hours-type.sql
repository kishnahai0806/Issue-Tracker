--liquibase formatted sql
--changeset krish:006-fix-avg-resolution-hours-type
-- AnalyticsSnapshot.avgResolutionHours is mapped as Double; DECIMAL(10,2) caused a
-- Hibernate type mismatch under schema validation, so the column must stay floating-point.
ALTER TABLE analytics_snapshots
ALTER COLUMN avg_resolution_hours
TYPE DOUBLE PRECISION;
