--liquibase formatted sql

--changeset krish:004-schema-corrections
-- Drop the incorrect global user role because organization_members.role is the authorization source per tenant.
ALTER TABLE users DROP COLUMN IF EXISTS role;

-- Add a per-project counter so issue numbers can be allocated atomically under concurrent requests.
ALTER TABLE projects ADD COLUMN IF NOT EXISTS next_issue_number BIGINT NOT NULL DEFAULT 0;

-- Normalize the issue number uniqueness constraint to the reviewed stable name.
ALTER TABLE issues DROP CONSTRAINT IF EXISTS issues_project_id_issue_number_key;
ALTER TABLE issues DROP CONSTRAINT IF EXISTS uq_issues_project_issue_number;
ALTER TABLE issues ADD CONSTRAINT uq_issues_project_issue_number UNIQUE (project_id, issue_number);

-- Add optimistic locking support for future Hibernate @Version mapping.
ALTER TABLE issues ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add soft-delete state so issue history can be preserved instead of hard-deleted.
ALTER TABLE issues ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Index active issues because normal issue queries filter for rows where deleted_at is null.
CREATE INDEX IF NOT EXISTS idx_issues_deleted_at ON issues(deleted_at) WHERE deleted_at IS NULL;

-- Normalize the label name uniqueness constraint to the reviewed stable name.
ALTER TABLE labels DROP CONSTRAINT IF EXISTS labels_project_id_name_key;
ALTER TABLE labels DROP CONSTRAINT IF EXISTS uq_labels_project_name;
ALTER TABLE labels ADD CONSTRAINT uq_labels_project_name UNIQUE (project_id, name);

-- Switch organization membership identity to the tenant/user pair when it is not already composite.
ALTER TABLE organization_members DROP CONSTRAINT IF EXISTS organization_members_pkey;
ALTER TABLE organization_members DROP CONSTRAINT IF EXISTS organization_members_organization_id_user_id_key;
ALTER TABLE organization_members DROP CONSTRAINT IF EXISTS pk_organization_members;
ALTER TABLE organization_members ADD CONSTRAINT pk_organization_members PRIMARY KEY (organization_id, user_id);

-- Skipped projects organization/key uniqueness because migration 001 already creates UNIQUE (organization_id, key).
