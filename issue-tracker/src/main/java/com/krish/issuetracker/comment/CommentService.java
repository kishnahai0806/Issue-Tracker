package com.krish.issuetracker.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAuditLog;
import com.krish.issuetracker.domain.entity.IssueComment;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.exception.AccessDeniedException;
import com.krish.issuetracker.exception.CommentNotFoundException;
import com.krish.issuetracker.exception.IssueNotFoundException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.issue.dto.CommentResponse;
import com.krish.issuetracker.issue.dto.CreateCommentRequest;
import com.krish.issuetracker.issue.dto.UpdateCommentRequest;
import com.krish.issuetracker.notification.NotificationEventPublisher;
import com.krish.issuetracker.repository.IssueAuditLogRepository;
import com.krish.issuetracker.repository.IssueCommentRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.IssueWatcherRepository;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
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
	private final IssueWatcherRepository issueWatcherRepository;
	private final OrganizationMemberRepository organizationMemberRepository;
	private final UserRepository userRepository;
	private final NotificationEventPublisher notificationEventPublisher;
	private final MeterRegistry meterRegistry;

	public CommentService(
			IssueCommentRepository issueCommentRepository,
			IssueAuditLogRepository issueAuditLogRepository,
			IssueRepository issueRepository,
			ProjectRepository projectRepository,
			IssueWatcherRepository issueWatcherRepository,
			OrganizationMemberRepository organizationMemberRepository,
			UserRepository userRepository,
			NotificationEventPublisher notificationEventPublisher,
			MeterRegistry meterRegistry) {
		this.issueCommentRepository = issueCommentRepository;
		this.issueAuditLogRepository = issueAuditLogRepository;
		this.issueRepository = issueRepository;
		this.projectRepository = projectRepository;
		this.issueWatcherRepository = issueWatcherRepository;
		this.organizationMemberRepository = organizationMemberRepository;
		this.userRepository = userRepository;
		this.notificationEventPublisher = notificationEventPublisher;
		this.meterRegistry = meterRegistry;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public CommentResponse addComment(
			UUID orgId,
			UUID projectId,
			UUID issueId,
			CreateCommentRequest request,
			UUID authorId) {
		Project project = loadProject(orgId, projectId);
		Issue issue = loadIssue(projectId, issueId);

		IssueComment comment = new IssueComment();
		comment.setIssueId(issueId);
		comment.setAuthorId(authorId);
		comment.setContent(request.content());

		IssueComment savedComment = issueCommentRepository.save(comment);
		meterRegistry.counter("comments.added").increment();
		issueAuditLogRepository.save(createAuditLog(issueId, authorId, "comment_added", null, savedComment.getId()));
		publishCommentAddedNotifications(orgId, project, issue, savedComment, authorId);

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

	private Project loadProject(UUID orgId, UUID projectId) {
		return projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private void verifyProjectAccess(UUID orgId, UUID projectId) {
		loadProject(orgId, projectId);
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

	private void publishCommentAddedNotifications(
			UUID orgId,
			Project project,
			Issue issue,
			IssueComment comment,
			UUID authorId) {
		issueWatcherRepository.findAllByIdIssueId(issue.getId())
				.stream()
				.map(watcher -> watcher.getId().getUserId())
				.filter(userId -> !authorId.equals(userId))
				.filter(userId -> isCurrentOrganizationMember(orgId, userId))
				.map(userRepository::findByIdAndIsActiveTrue)
				.flatMap(java.util.Optional::stream)
				.forEach(watcher -> notificationEventPublisher.publishEmailNotification(
						watcher.getEmail(),
						watcher.getFullName(),
						"New comment on " + issueKey(project, issue),
						"comment-added",
						Map.of(
								"recipientName", watcher.getFullName(),
								"issueKey", issueKey(project, issue),
								"issueTitle", issue.getTitle(),
								"projectName", project.getName(),
								"commentAuthor", displayName(authorId),
								"commentPreview", commentPreview(comment.getContent()),
								"issueUrl", issueUrl(orgId, project.getId(), issue.getId()))));
	}

	private boolean isCurrentOrganizationMember(UUID orgId, UUID userId) {
		return organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, userId);
	}

	private String displayName(UUID userId) {
		return userRepository.findById(userId)
				.map(User::getFullName)
				.orElse(userId.toString());
	}

	private String commentPreview(String content) {
		if (content.length() <= 200) {
			return content;
		}
		return content.substring(0, 200);
	}

	private String issueKey(Project project, Issue issue) {
		return project.getKey() + "-" + issue.getIssueNumber();
	}

	private String issueUrl(UUID orgId, UUID projectId, UUID issueId) {
		return "/api/v1/organizations/" + orgId + "/projects/" + projectId + "/issues/" + issueId;
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
