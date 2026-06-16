package com.krish.issuetracker.issue;

import java.util.Locale;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueLabel;
import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import com.krish.issuetracker.issue.dto.IssueFilterRequest;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class IssueSpecification {

	private IssueSpecification() {
	}

	public static Specification<Issue> hasStatus(IssueStatus status) {
		return (root, query, cb) -> {
			if (status == null) {
				return null;
			}
			return cb.equal(root.get("status"), status);
		};
	}

	public static Specification<Issue> hasPriority(IssuePriority priority) {
		return (root, query, cb) -> {
			if (priority == null) {
				return null;
			}
			return cb.equal(root.get("priority"), priority);
		};
	}

	public static Specification<Issue> hasType(IssueType type) {
		return (root, query, cb) -> {
			if (type == null) {
				return null;
			}
			return cb.equal(root.get("type"), type);
		};
	}

	public static Specification<Issue> hasAssignee(UUID assigneeId) {
		return (root, query, cb) -> {
			if (assigneeId == null) {
				return null;
			}
			return cb.equal(root.get("assigneeId"), assigneeId);
		};
	}

	public static Specification<Issue> hasLabel(UUID labelId) {
		return (root, query, cb) -> {
			if (labelId == null) {
				return null;
			}

			query.distinct(true);
			Subquery<UUID> subquery = query.subquery(UUID.class);
			Root<IssueLabel> issueLabelRoot = subquery.from(IssueLabel.class);
			subquery.select(issueLabelRoot.get("id").get("issueId").as(UUID.class));
			subquery.where(cb.equal(issueLabelRoot.get("id").get("labelId"), labelId));
			return root.get("id").in(subquery);
		};
	}

	public static Specification<Issue> titleContains(String search) {
		return (root, query, cb) -> {
			if (search == null || search.isBlank()) {
				return null;
			}
			return cb.like(
					cb.lower(root.get("title")),
					"%" + search.toLowerCase(Locale.ROOT) + "%");
		};
	}

	public static Specification<Issue> isNotDeleted() {
		return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
	}

	public static Specification<Issue> belongsToProject(UUID projectId) {
		return (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
	}

	public static Specification<Issue> buildFilter(IssueFilterRequest filter) {
		return Specification
				.where(belongsToProject(filter.projectId()))
				.and(isNotDeleted())
				.and(hasStatus(filter.status()))
				.and(hasPriority(filter.priority()))
				.and(hasType(filter.type()))
				.and(hasAssignee(filter.assigneeId()))
				.and(hasLabel(filter.labelId()))
				.and(titleContains(filter.search()));
	}
}
