package com.krish.issuetracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {

	List<IssueComment> findAllByIssueIdOrderByCreatedAtAsc(UUID issueId);

	Optional<IssueComment> findByIdAndIssueId(UUID commentId, UUID issueId);
}
