--liquibase formatted sql

--changeset krish:001-create-pgcrypto-extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

--changeset krish:002-create-user-role-enum splitStatements:false
DO $$
BEGIN
	CREATE TYPE user_role AS ENUM ('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'REPORTER');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END
$$;

--changeset krish:003-create-organizations-table
CREATE TABLE organizations (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	name VARCHAR(255) NOT NULL UNIQUE,
	slug VARCHAR(100) NOT NULL UNIQUE,
	plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
	is_active BOOLEAN NOT NULL DEFAULT true,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset krish:004-create-users-table
CREATE TABLE users (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	email VARCHAR(255) NOT NULL UNIQUE,
	password_hash VARCHAR(255) NOT NULL,
	full_name VARCHAR(255) NOT NULL,
	avatar_url VARCHAR(500),
	is_active BOOLEAN NOT NULL DEFAULT true,
	email_verified BOOLEAN NOT NULL DEFAULT false,
	last_login_at TIMESTAMP,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset krish:005-create-organization-members-table
CREATE TABLE organization_members (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	role user_role NOT NULL DEFAULT 'DEVELOPER',
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE (organization_id, user_id)
);

--changeset krish:006-create-projects-table
CREATE TABLE projects (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
	name VARCHAR(255) NOT NULL,
	key VARCHAR(10) NOT NULL,
	description TEXT,
	is_archived BOOLEAN NOT NULL DEFAULT false,
	created_by UUID NOT NULL REFERENCES users(id),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE (organization_id, key)
);
