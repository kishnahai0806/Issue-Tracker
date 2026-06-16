package com.krish.issuetracker.repository;

import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	Optional<Project> findByIdAndOrganizationIdAndIsArchivedFalse(UUID id, UUID organizationId);
}
