--liquibase formatted sql

--changeset krish:007-create-issue-status-enum splitStatements:false
DO $$
BEGIN
	CREATE TYPE issue_status AS ENUM ('BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CLOSED', 'CANCELLED');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END
$$;

--changeset krish:008-create-issue-priority-enum splitStatements:false
DO $$
BEGIN
	CREATE TYPE issue_priority AS ENUM ('LOWEST', 'LOW', 'MEDIUM', 'HIGH', 'HIGHEST', 'CRITICAL');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END
$$;

--changeset krish:009-create-issue-type-enum splitStatements:false
DO $$
BEGIN
	CREATE TYPE issue_type AS ENUM ('BUG', 'FEATURE', 'TASK', 'IMPROVEMENT', 'EPIC', 'SUBTASK');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END
$$;

--changeset krish:010-create-issues-table
CREATE TABLE issues (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
	issue_number INTEGER NOT NULL,
	title VARCHAR(500) NOT NULL,
	description TEXT,
	status issue_status NOT NULL DEFAULT 'BACKLOG',
	priority issue_priority NOT NULL DEFAULT 'MEDIUM',
	type issue_type NOT NULL DEFAULT 'TASK',
	reporter_id UUID NOT NULL REFERENCES users(id),
	assignee_id UUID REFERENCES users(id),
	parent_issue_id UUID REFERENCES issues(id),
	story_points INTEGER,
	due_date DATE,
	resolved_at TIMESTAMP,
	closed_at TIMESTAMP,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE (project_id, issue_number)
);

--changeset krish:011-create-issue-comments-table
CREATE TABLE issue_comments (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
	author_id UUID NOT NULL REFERENCES users(id),
	content TEXT NOT NULL,
	is_edited BOOLEAN NOT NULL DEFAULT false,
	edited_at TIMESTAMP,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset krish:012-create-labels-table
CREATE TABLE labels (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
	name VARCHAR(100) NOT NULL,
	color_hex VARCHAR(7) NOT NULL DEFAULT '#808080',
	UNIQUE (project_id, name)
);

--changeset krish:013-create-issue-labels-table
CREATE TABLE issue_labels (
	issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
	label_id UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
	PRIMARY KEY (issue_id, label_id)
);

--changeset krish:014-create-issue-watchers-table
CREATE TABLE issue_watchers (
	issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	PRIMARY KEY (issue_id, user_id)
);

--changeset krish:015-create-issue-attachments-table
CREATE TABLE issue_attachments (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
	file_name VARCHAR(255) NOT NULL,
	file_size_bytes BIGINT NOT NULL,
	content_type VARCHAR(100) NOT NULL,
	storage_key VARCHAR(500) NOT NULL,
	uploaded_by UUID NOT NULL REFERENCES users(id),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
