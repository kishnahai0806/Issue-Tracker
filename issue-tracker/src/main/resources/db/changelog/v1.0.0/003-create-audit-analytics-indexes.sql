--liquibase formatted sql

--changeset krish:016-create-issue-audit-log-table
CREATE TABLE issue_audit_log (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
	changed_by UUID NOT NULL REFERENCES users(id),
	field_name VARCHAR(100) NOT NULL,
	old_value TEXT,
	new_value TEXT,
	changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset krish:017-create-analytics-snapshots-table
CREATE TABLE analytics_snapshots (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	project_id UUID NOT NULL REFERENCES projects(id),
	snapshot_date DATE NOT NULL,
	total_issues INTEGER NOT NULL DEFAULT 0,
	open_issues INTEGER NOT NULL DEFAULT 0,
	closed_issues INTEGER NOT NULL DEFAULT 0,
	avg_resolution_hours DECIMAL(10,2),
	issues_by_priority JSONB,
	issues_by_type JSONB,
	issues_by_assignee JSONB,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE (project_id, snapshot_date)
);

--changeset krish:018-create-core-performance-indexes
CREATE INDEX idx_org_members_user ON organization_members(user_id);
CREATE INDEX idx_issues_project_id ON issues(project_id);
CREATE INDEX idx_issues_assignee_id ON issues(assignee_id);
CREATE INDEX idx_issues_status ON issues(status);
CREATE INDEX idx_issues_project_status ON issues(project_id, status);
CREATE INDEX idx_issues_created_at ON issues(created_at DESC);
CREATE INDEX idx_issues_updated_at ON issues(updated_at DESC);
CREATE INDEX idx_comments_issue_id ON issue_comments(issue_id);
CREATE INDEX idx_audit_log_issue_id ON issue_audit_log(issue_id);
CREATE INDEX idx_audit_log_changed_at ON issue_audit_log(changed_at);
CREATE INDEX idx_snapshots_project_date ON analytics_snapshots(project_id, snapshot_date DESC);
