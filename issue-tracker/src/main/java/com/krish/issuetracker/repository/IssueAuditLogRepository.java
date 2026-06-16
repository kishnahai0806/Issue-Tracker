package com.krish.issuetracker.repository;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.IssueAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueAuditLogRepository extends JpaRepository<IssueAuditLog, UUID> {

	List<IssueAuditLog> findAllByIssueIdOrderByChangedAtDesc(UUID issueId);
}
