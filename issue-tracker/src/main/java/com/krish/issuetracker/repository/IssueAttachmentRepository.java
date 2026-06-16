package com.krish.issuetracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.IssueAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueAttachmentRepository extends JpaRepository<IssueAttachment, UUID> {

	List<IssueAttachment> findAllByIssueId(UUID issueId);

	Optional<IssueAttachment> findByIdAndIssueId(UUID attachmentId, UUID issueId);
}
