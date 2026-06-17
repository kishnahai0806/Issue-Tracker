package com.krish.issuetracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

	Optional<Issue> findByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);

	boolean existsByProjectIdAndIssueNumber(UUID projectId, int issueNumber);

	Page<Issue> findAllByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

	@Query("""
			SELECT COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			""")
	long countByProjectId(@Param("projectId") UUID projectId);

	@Query("""
			SELECT COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			AND i.status NOT IN (
				com.krish.issuetracker.domain.enums.IssueStatus.DONE,
				com.krish.issuetracker.domain.enums.IssueStatus.CLOSED,
				com.krish.issuetracker.domain.enums.IssueStatus.CANCELLED
			)
			""")
	long countOpenByProjectId(@Param("projectId") UUID projectId);

	@Query("""
			SELECT COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			AND i.status = com.krish.issuetracker.domain.enums.IssueStatus.IN_PROGRESS
			""")
	long countInProgressByProjectId(@Param("projectId") UUID projectId);

	@Query("""
			SELECT COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			AND i.status IN (
				com.krish.issuetracker.domain.enums.IssueStatus.DONE,
				com.krish.issuetracker.domain.enums.IssueStatus.CLOSED
			)
			""")
	long countClosedByProjectId(@Param("projectId") UUID projectId);

	@Query("""
			SELECT AVG((FUNCTION('date_part', 'epoch', i.resolvedAt) - FUNCTION('date_part', 'epoch', i.createdAt)) / 3600.0)
			FROM Issue i
			WHERE i.projectId = :projectId
			AND i.resolvedAt IS NOT NULL
			AND i.deletedAt IS NULL
			""")
	Double avgResolutionHoursByProjectId(@Param("projectId") UUID projectId);

	@Query("""
			SELECT i.priority, COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			GROUP BY i.priority
			""")
	List<Object[]> countByPriorityForProject(@Param("projectId") UUID projectId);

	@Query("""
			SELECT i.type, COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			GROUP BY i.type
			""")
	List<Object[]> countByTypeForProject(@Param("projectId") UUID projectId);

	@Query("""
			SELECT i.assigneeId, COUNT(i) FROM Issue i
			WHERE i.projectId = :projectId
			AND i.deletedAt IS NULL
			AND i.assigneeId IS NOT NULL
			GROUP BY i.assigneeId
			""")
	List<Object[]> countByAssigneeForProject(@Param("projectId") UUID projectId);

	/*
	 * Label filter Specifications MUST call
	 * query.distinct(true) to prevent count inflation
	 * from the many-to-many issue_labels join.
	 */
	@Override
	long count(Specification<Issue> spec);
}
