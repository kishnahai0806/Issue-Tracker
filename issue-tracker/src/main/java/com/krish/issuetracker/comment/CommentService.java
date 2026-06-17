package com.krish.issuetracker.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAuditLog;
import com.krish.issuetracker.domain.entity.IssueComment;
import com.krish.issuetracker.exception.AccessDeniedException;
import com.krish.issuetracker.exception.CommentNotFoundException;
import com.krish.issuetracker.exception.IssueNotFoundException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.issue.dto.CommentResponse;
import com.krish.issuetracker.issue.dto.CreateCommentRequest;
import com.krish.issuetracker.issue.dto.UpdateCommentRequest;
import com.krish.issuetracker.repository.IssueAuditLogRepository;
import com.krish.issuetracker.repository.IssueCommentRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CommentService {

	private final IssueCommentRepository issueCommentRepository;
	private final IssueAuditLogRepository issueAuditLogRepository;
	private final IssueRepository issueRepository;
	private final ProjectRepository projectRepository;

	public CommentService(
			IssueCommentRepository issueCommentRepository,
			IssueAuditLogRepository issueAuditLogRepository,
			IssueRepository issueRepository,
			ProjectRepository projectRepository) {
		this.issueCommentRepository = issueCommentRepository;
		this.issueAuditLogRepository = issueAuditLogRepository;
		this.issueRepository = issueRepository;
		this.projectRepository = projectRepository;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public CommentResponse addComment(
			UUID orgId,
			UUID projectId,
			UUID issueId,
			CreateCommentRequest request,
			UUID authorId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);

		IssueComment comment = new IssueComment();
		comment.setIssueId(issueId);
		comment.setAuthorId(authorId);
		comment.setContent(request.content());

		IssueComment savedComment = issueCommentRepository.save(comment);
		issueAuditLogRepository.save(createAuditLog(issueId, authorId, "comment_added", null, savedComment.getId()));

		log.info("Comment added: {} to issue {}", savedComment.getId(), issueId);
		return toCommentResponse(savedComment);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public CommentResponse updateComment(
			UUID orgId,
			UUID projectId,
			UUID issueId,
			UUID commentId,
			UpdateCommentRequest request,
			UUID requestingUserId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		IssueComment comment = loadComment(issueId, commentId);
		verifyAuthor(comment, requestingUserId);

		comment.setContent(request.content());
		comment.setEdited(true);
		comment.setEditedAt(LocalDateTime.now());

		IssueComment savedComment = issueCommentRepository.save(comment);
		log.info("Comment updated: {}", commentId);
		return toCommentResponse(savedComment);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public void deleteComment(UUID orgId, UUID projectId, UUID issueId, UUID commentId, UUID requestingUserId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		IssueComment comment = loadComment(issueId, commentId);
		verifyAuthor(comment, requestingUserId);

		issueCommentRepository.delete(comment);
		log.info("Comment deleted: {}", commentId);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public List<CommentResponse> getComments(UUID orgId, UUID projectId, UUID issueId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		return issueCommentRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId)
				.stream()
				.map(this::toCommentResponse)
				.toList();
	}

	private void verifyProjectAccess(UUID orgId, UUID projectId) {
		projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private Issue loadIssue(UUID projectId, UUID issueId) {
		return issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId)
				.orElseThrow(() -> new IssueNotFoundException(issueId));
	}

	private IssueComment loadComment(UUID issueId, UUID commentId) {
		return issueCommentRepository.findByIdAndIssueId(commentId, issueId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
	}

	private void verifyAuthor(IssueComment comment, UUID requestingUserId) {
		if (!requestingUserId.equals(comment.getAuthorId())) {
			throw new AccessDeniedException();
		}
	}

	private IssueAuditLog createAuditLog(UUID issueId, UUID changedBy, String fieldName, Object oldValue, Object newValue) {
		IssueAuditLog auditLog = new IssueAuditLog();
		auditLog.setIssueId(issueId);
		auditLog.setChangedBy(changedBy);
		auditLog.setFieldName(fieldName);
		auditLog.setOldValue(toAuditValue(oldValue));
		auditLog.setNewValue(toAuditValue(newValue));
		return auditLog;
	}

	private String toAuditValue(Object value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	private CommentResponse toCommentResponse(IssueComment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getIssueId(),
				comment.getAuthorId(),
				comment.getContent(),
				comment.isEdited(),
				comment.getEditedAt(),
				comment.getCreatedAt());
	}
}
