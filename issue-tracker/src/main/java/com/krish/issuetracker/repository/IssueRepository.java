package com.krish.issuetracker.repository;

import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

	Optional<Issue> findByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);

	boolean existsByProjectIdAndIssueNumber(UUID projectId, int issueNumber);

	Page<Issue> findAllByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

	/*
	 * Label filter Specifications MUST call
	 * query.distinct(true) to prevent count inflation
	 * from the many-to-many issue_labels join.
	 */
	@Override
	long count(Specification<Issue> spec);
}
