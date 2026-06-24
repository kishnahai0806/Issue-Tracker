package com.krish.issuetracker.repository;

import java.util.UUID;
import java.util.List;

import com.krish.issuetracker.domain.entity.IssueWatcher;
import com.krish.issuetracker.domain.entity.IssueWatcherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueWatcherRepository extends JpaRepository<IssueWatcher, IssueWatcherId> {

	boolean existsByIdIssueIdAndIdUserId(UUID issueId, UUID userId);

	void deleteByIdIssueIdAndIdUserId(UUID issueId, UUID userId);

	List<IssueWatcher> findAllByIdIssueId(UUID issueId);

	@Modifying
	@Query("""
			DELETE FROM IssueWatcher watcher
			WHERE watcher.id.userId = :userId
			AND watcher.id.issueId IN (
				SELECT issue.id
				FROM Issue issue
				WHERE issue.projectId IN (
					SELECT project.id
					FROM Project project
					WHERE project.organizationId = :organizationId
				)
			)
			""")
	int deleteByOrganizationIdAndUserId(
			@Param("organizationId") UUID organizationId,
			@Param("userId") UUID userId);
}
