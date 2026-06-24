package com.krish.issuetracker.issue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAuditLog;
import com.krish.issuetracker.domain.entity.IssueComment;
import com.krish.issuetracker.domain.entity.IssueLabel;
import com.krish.issuetracker.domain.entity.IssueLabelId;
import com.krish.issuetracker.domain.entity.IssueWatcher;
import com.krish.issuetracker.domain.entity.IssueWatcherId;
import com.krish.issuetracker.domain.entity.Label;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.exception.AssigneeNotMemberException;
import com.krish.issuetracker.exception.IssueNotFoundException;
import com.krish.issuetracker.exception.LabelNotInProjectException;
import com.krish.issuetracker.exception.OptimisticLockException;
import com.krish.issuetracker.exception.ParentIssueNotFoundException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.exception.WatcherNotMemberException;
import com.krish.issuetracker.issue.dto.AuditLogResponse;
import com.krish.issuetracker.issue.dto.CommentResponse;
import com.krish.issuetracker.issue.dto.CreateIssueRequest;
import com.krish.issuetracker.issue.dto.IssueDetailResponse;
import com.krish.issuetracker.issue.dto.IssueFilterRequest;
import com.krish.issuetracker.issue.dto.IssueResponse;
import com.krish.issuetracker.issue.dto.IssueSummaryResponse;
import com.krish.issuetracker.issue.dto.PagedIssueResponse;
import com.krish.issuetracker.issue.dto.UpdateIssueRequest;
import com.krish.issuetracker.label.dto.LabelResponse;
import com.krish.issuetracker.notification.NotificationEventPublisher;
import com.krish.issuetracker.repository.IssueAuditLogRepository;
import com.krish.issuetracker.repository.IssueCommentRepository;
import com.krish.issuetracker.repository.IssueLabelRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.IssueWatcherRepository;
import com.krish.issuetracker.repository.LabelRepository;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.websocket.IssueUpdateEvent;
import com.krish.issuetracker.websocket.WebSocketEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class IssueService {

	private final IssueRepository issueRepository;
	private final IssueCommentRepository issueCommentRepository;
	private final IssueLabelRepository issueLabelRepository;
	private final IssueWatcherRepository issueWatcherRepository;
	private final IssueAuditLogRepository issueAuditLogRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final OrganizationMemberRepository organizationMemberRepository;
	private final LabelRepository labelRepository;
	private final IssueNumberGenerator issueNumberGenerator;
	private final WebSocketEventPublisher eventPublisher;
	private final NotificationEventPublisher notificationEventPublisher;
	private final MeterRegistry meterRegistry;

	public IssueService(
			IssueRepository issueRepository,
			IssueCommentRepository issueCommentRepository,
			IssueLabelRepository issueLabelRepository,
			IssueWatcherRepository issueWatcherRepository,
			IssueAuditLogRepository issueAuditLogRepository,
			ProjectRepository projectRepository,
			UserRepository userRepository,
			OrganizationMemberRepository organizationMemberRepository,
			LabelRepository labelRepository,
			IssueNumberGenerator issueNumberGenerator,
			WebSocketEventPublisher eventPublisher,
			NotificationEventPublisher notificationEventPublisher,
			MeterRegistry meterRegistry) {
		this.issueRepository = issueRepository;
		this.issueCommentRepository = issueCommentRepository;
		this.issueLabelRepository = issueLabelRepository;
		this.issueWatcherRepository = issueWatcherRepository;
		this.issueAuditLogRepository = issueAuditLogRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.organizationMemberRepository = organizationMemberRepository;
		this.labelRepository = labelRepository;
		this.issueNumberGenerator = issueNumberGenerator;
		this.eventPublisher = eventPublisher;
		this.notificationEventPublisher = notificationEventPublisher;
		this.meterRegistry = meterRegistry;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DEVELOPER')")
	public IssueResponse createIssue(UUID orgId, UUID projectId, CreateIssueRequest request, UUID reporterId) {
		try {
			Project project = projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId)
					.orElseThrow(() -> new ProjectNotFoundException(projectId));
			validateAssigneeMembership(project, request.assigneeId());
			validateParentIssue(projectId, request.parentIssueId());

			int issueNumber = issueNumberGenerator.generateNextIssueNumber(projectId);

			Issue issue = new Issue();
			issue.setProjectId(projectId);
			issue.setIssueNumber(issueNumber);
			issue.setTitle(request.title());
			issue.setDescription(request.description());
			issue.setStatus(request.status() == null ? IssueStatus.BACKLOG : request.status());
			issue.setPriority(request.priority());
			issue.setType(request.type());
			issue.setReporterId(reporterId);
			issue.setAssigneeId(request.assigneeId());
			issue.setParentIssueId(request.parentIssueId());
			issue.setStoryPoints(request.storyPoints());
			issue.setDueDate(request.dueDate());

			Issue savedIssue = issueRepository.save(issue);
			meterRegistry.counter("issues.created").increment();
			saveIssueLabels(savedIssue, request.labelIds());
			saveCreationAuditLog(savedIssue, reporterId);
			eventPublisher.publishIssueUpdate(new IssueUpdateEvent(
					savedIssue.getProjectId(),
					savedIssue.getId(),
					"CREATED",
					reporterId));

			log.info("Issue created: {} in project {}", savedIssue.getId(), projectId);
			return toIssueResponse(savedIssue, loadLabelResponses(savedIssue));
		} catch (ObjectOptimisticLockingFailureException ex) {
			throw new OptimisticLockException();
		}
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DEVELOPER')")
	public IssueDetailResponse getIssue(UUID orgId, UUID projectId, UUID issueId) {
		verifyProjectAccess(orgId, projectId);
		Issue issue = loadIssue(projectId, issueId);
		List<CommentResponse> comments = issueCommentRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId)
				.stream()
				.map(this::toCommentResponse)
				.toList();
		List<AuditLogResponse> auditLog = issueAuditLogRepository.findAllByIssueIdOrderByChangedAtDesc(issueId)
				.stream()
				.map(this::toAuditLogResponse)
				.toList();
		List<UUID> watchers = issueWatcherRepository.findAllByIdIssueId(issueId)
				.stream()
				.map(watcher -> watcher.getId().getUserId())
				.toList();

		return toIssueDetailResponse(issue, loadLabelResponses(issue), comments, auditLog, watchers);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public PagedIssueResponse listIssues(IssueFilterRequest filter, UUID orgId) {
		verifyProjectAccess(orgId, filter.projectId());
		Specification<Issue> specification = IssueSpecification.buildFilter(filter);
		Page<Issue> page = issueRepository.findAll(specification, PageRequest.of(filter.page(), filter.size()));
		List<IssueSummaryResponse> content = page.getContent()
				.stream()
				.map(this::toIssueSummaryResponse)
				.toList();

		return new PagedIssueResponse(
				content,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isLast());
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DEVELOPER')")
	public IssueResponse updateIssue(
			UUID orgId,
			UUID projectId,
			UUID issueId,
			UpdateIssueRequest request,
			UUID requestingUserId) {
		try {
			Project project = loadProject(orgId, projectId);
			Issue issue = loadIssue(projectId, issueId);
			validateAssigneeMembership(project, request.assigneeId());
			validateParentIssue(projectId, request.parentIssueId());
			if (!Objects.equals(issue.getVersion(), request.version())) {
				throw new OptimisticLockException();
			}
			IssueStatus previousStatus = issue.getStatus();
			UUID previousAssigneeId = issue.getAssigneeId();

			List<IssueAuditLog> auditLogs = new ArrayList<>();
			applyIssueUpdates(issue, request, requestingUserId, auditLogs);

			Issue savedIssue = issueRepository.saveAndFlush(issue);
			issueAuditLogRepository.saveAll(auditLogs);
			boolean statusChanged = request.status() != null && !Objects.equals(previousStatus, savedIssue.getStatus());
			boolean assigneeChanged = request.assigneeId() != null
					&& !Objects.equals(previousAssigneeId, savedIssue.getAssigneeId());
			if (statusChanged && (savedIssue.getStatus() == IssueStatus.DONE || savedIssue.getStatus() == IssueStatus.CLOSED)) {
				meterRegistry.counter("issues.closed").increment();
			}
			String eventType = statusChanged && (savedIssue.getStatus() == IssueStatus.DONE || savedIssue.getStatus() == IssueStatus.CLOSED)
					? "STATUS_CHANGED"
					: "UPDATED";
			eventPublisher.publishIssueUpdate(new IssueUpdateEvent(
					savedIssue.getProjectId(),
					savedIssue.getId(),
					eventType,
					requestingUserId));
			if (assigneeChanged) {
				publishIssueAssignedNotification(orgId, project, savedIssue, requestingUserId);
			}
			if (statusChanged) {
				publishStatusChangedNotifications(orgId, project, savedIssue, previousStatus, requestingUserId);
			}
			return toIssueResponse(savedIssue, loadLabelResponses(savedIssue));
		} catch (ObjectOptimisticLockingFailureException ex) {
			throw new OptimisticLockException();
		}
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public void deleteIssue(UUID orgId, UUID projectId, UUID issueId, UUID requestingUserId) {
		try {
			verifyProjectAccess(orgId, projectId);
			Issue issue = loadIssue(projectId, issueId);
			issue.setDeletedAt(OffsetDateTime.now());
			issueRepository.saveAndFlush(issue);
			issueAuditLogRepository.save(createAuditLog(issue.getId(), requestingUserId, "deleted", null, "true"));
			eventPublisher.publishIssueUpdate(new IssueUpdateEvent(
					issue.getProjectId(),
					issue.getId(),
					"DELETED",
					requestingUserId));
		} catch (ObjectOptimisticLockingFailureException ex) {
			throw new OptimisticLockException();
		}
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public void addWatcher(UUID orgId, UUID projectId, UUID issueId, UUID userId) {
		Project project = loadProject(orgId, projectId);
		loadIssue(projectId, issueId);
		validateWatcherMembership(project, userId);
		if (issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, userId)) {
			return;
		}

		IssueWatcher watcher = new IssueWatcher();
		watcher.setId(new IssueWatcherId(issueId, userId));
		issueWatcherRepository.save(watcher);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public void removeWatcher(UUID orgId, UUID projectId, UUID issueId, UUID userId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		issueWatcherRepository.deleteByIdIssueIdAndIdUserId(issueId, userId);
	}

	private void saveIssueLabels(Issue issue, List<UUID> labelIds) {
		if (labelIds == null || labelIds.isEmpty()) {
			return;
		}

		List<Label> labels = loadProjectLabels(issue.getProjectId(), labelIds);
		List<IssueLabel> issueLabels = labels.stream()
				.map(label -> {
					IssueLabel issueLabel = new IssueLabel();
					issueLabel.setId(new IssueLabelId(issue.getId(), label.getId()));
					return issueLabel;
				})
				.toList();
		issueLabelRepository.saveAll(issueLabels);
	}

	private void validateAssigneeMembership(Project project, UUID assigneeId) {
		if (assigneeId == null) {
			return;
		}

		boolean isMember = organizationMemberRepository.existsById_OrganizationIdAndId_UserId(
				project.getOrganizationId(),
				assigneeId);
		if (!isMember) {
			throw new AssigneeNotMemberException();
		}
	}

	private void validateWatcherMembership(Project project, UUID userId) {
		boolean isMember = organizationMemberRepository.existsById_OrganizationIdAndId_UserId(
				project.getOrganizationId(),
				userId);
		if (!isMember) {
			throw new WatcherNotMemberException();
		}
	}

	private void validateParentIssue(UUID projectId, UUID parentIssueId) {
		if (parentIssueId == null) {
			return;
		}

		issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(parentIssueId, projectId)
				.orElseThrow(ParentIssueNotFoundException::new);
	}

	private List<Label> loadProjectLabels(UUID projectId, List<UUID> labelIds) {
		List<UUID> distinctLabelIds = labelIds.stream()
				.distinct()
				.toList();
		List<Label> labels = labelRepository.findAllById(distinctLabelIds);
		boolean allLabelsBelongToProject = labels.size() == distinctLabelIds.size()
				&& labels.stream().allMatch(label -> projectId.equals(label.getProjectId()));
		if (!allLabelsBelongToProject) {
			throw new LabelNotInProjectException();
		}
		return labels;
	}

	private void saveCreationAuditLog(Issue issue, UUID reporterId) {
		IssueAuditLog auditLog = new IssueAuditLog();
		auditLog.setIssueId(issue.getId());
		auditLog.setChangedBy(reporterId);
		auditLog.setFieldName("created");
		auditLog.setOldValue(null);
		auditLog.setNewValue(issue.getStatus().name());
		issueAuditLogRepository.save(auditLog);
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

	private void publishIssueAssignedNotification(UUID orgId, Project project, Issue issue, UUID assignedBy) {
		userRepository.findByIdAndIsActiveTrue(issue.getAssigneeId())
				.ifPresent(assignee -> notificationEventPublisher.publishEmailNotification(
						assignee.getEmail(),
						assignee.getFullName(),
						"You were assigned to " + issueKey(project, issue),
						"issue-assigned",
						Map.of(
								"recipientName", assignee.getFullName(),
								"issueKey", issueKey(project, issue),
								"issueTitle", issue.getTitle(),
								"projectName", project.getName(),
								"assignedBy", displayName(assignedBy),
								"issueUrl", issueUrl(orgId, project.getId(), issue.getId()))));
	}

	private void publishStatusChangedNotifications(
			UUID orgId,
			Project project,
			Issue issue,
			IssueStatus previousStatus,
			UUID changedBy) {
		issueWatcherRepository.findAllByIdIssueId(issue.getId())
				.stream()
				.map(watcher -> watcher.getId().getUserId())
				.map(userRepository::findByIdAndIsActiveTrue)
				.flatMap(java.util.Optional::stream)
				.forEach(watcher -> notificationEventPublisher.publishEmailNotification(
						watcher.getEmail(),
						watcher.getFullName(),
						"Status changed for " + issueKey(project, issue),
						"status-changed",
						Map.of(
								"recipientName", watcher.getFullName(),
								"issueKey", issueKey(project, issue),
								"issueTitle", issue.getTitle(),
								"projectName", project.getName(),
								"oldStatus", previousStatus.name(),
								"newStatus", issue.getStatus().name(),
								"changedBy", displayName(changedBy),
								"issueUrl", issueUrl(orgId, project.getId(), issue.getId()))));
	}

	private String displayName(UUID userId) {
		return userRepository.findById(userId)
				.map(User::getFullName)
				.orElse(userId.toString());
	}

	private String issueKey(Project project, Issue issue) {
		return project.getKey() + "-" + issue.getIssueNumber();
	}

	private String issueUrl(UUID orgId, UUID projectId, UUID issueId) {
		return "/api/v1/organizations/" + orgId + "/projects/" + projectId + "/issues/" + issueId;
	}

	private void applyIssueUpdates(
			Issue issue,
			UpdateIssueRequest request,
			UUID requestingUserId,
			List<IssueAuditLog> auditLogs) {
		if (request.title() != null && !Objects.equals(issue.getTitle(), request.title())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "title", issue.getTitle(), request.title()));
			issue.setTitle(request.title());
		}
		if (request.description() != null && !Objects.equals(issue.getDescription(), request.description())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "description", issue.getDescription(), request.description()));
			issue.setDescription(request.description());
		}
		if (request.status() != null && !Objects.equals(issue.getStatus(), request.status())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "status", issue.getStatus(), request.status()));
			issue.setStatus(request.status());
			if (request.status() == IssueStatus.DONE) {
				issue.setResolvedAt(LocalDateTime.now());
			}
			if (request.status() == IssueStatus.CLOSED) {
				issue.setClosedAt(LocalDateTime.now());
			}
		}
		if (request.priority() != null && !Objects.equals(issue.getPriority(), request.priority())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "priority", issue.getPriority(), request.priority()));
			issue.setPriority(request.priority());
		}
		if (request.type() != null && !Objects.equals(issue.getType(), request.type())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "type", issue.getType(), request.type()));
			issue.setType(request.type());
		}
		if (request.assigneeId() != null && !Objects.equals(issue.getAssigneeId(), request.assigneeId())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "assigneeId", issue.getAssigneeId(), request.assigneeId()));
			issue.setAssigneeId(request.assigneeId());
		}
		if (request.parentIssueId() != null && !Objects.equals(issue.getParentIssueId(), request.parentIssueId())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "parentIssueId", issue.getParentIssueId(), request.parentIssueId()));
			issue.setParentIssueId(request.parentIssueId());
		}
		if (request.storyPoints() != null && !Objects.equals(issue.getStoryPoints(), request.storyPoints())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "storyPoints", issue.getStoryPoints(), request.storyPoints()));
			issue.setStoryPoints(request.storyPoints());
		}
		if (request.dueDate() != null && !Objects.equals(issue.getDueDate(), request.dueDate())) {
			auditLogs.add(createAuditLog(issue.getId(), requestingUserId, "dueDate", issue.getDueDate(), request.dueDate()));
			issue.setDueDate(request.dueDate());
		}
	}

	private IssueAuditLog createAuditLog(
			UUID issueId,
			UUID changedBy,
			String fieldName,
			Object oldValue,
			Object newValue) {
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

	private List<LabelResponse> loadLabelResponses(Issue issue) {
		List<UUID> labelIds = issueLabelRepository.findAllByIdIssueId(issue.getId())
				.stream()
				.map(issueLabel -> issueLabel.getId().getLabelId())
				.toList();

		if (labelIds.isEmpty()) {
			return List.of();
		}

		return labelRepository.findAllById(labelIds)
				.stream()
				.map(label -> new LabelResponse(
						label.getId(),
						label.getProjectId(),
						label.getName(),
						label.getColorHex()))
				.toList();
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

	private AuditLogResponse toAuditLogResponse(IssueAuditLog auditLog) {
		return new AuditLogResponse(
				auditLog.getId(),
				auditLog.getIssueId(),
				auditLog.getChangedBy(),
				auditLog.getFieldName(),
				auditLog.getOldValue(),
				auditLog.getNewValue(),
				auditLog.getChangedAt());
	}

	private IssueSummaryResponse toIssueSummaryResponse(Issue issue) {
		return new IssueSummaryResponse(
				issue.getId(),
				issue.getProjectId(),
				issue.getIssueNumber(),
				issue.getTitle(),
				issue.getStatus(),
				issue.getPriority(),
				issue.getType(),
				issue.getAssigneeId(),
				issue.getDueDate(),
				issue.getUpdatedAt());
	}

	private IssueDetailResponse toIssueDetailResponse(
			Issue issue,
			List<LabelResponse> labels,
			List<CommentResponse> comments,
			List<AuditLogResponse> auditLog,
			List<UUID> watchers) {
		return new IssueDetailResponse(
				issue.getId(),
				issue.getProjectId(),
				issue.getIssueNumber(),
				issue.getTitle(),
				issue.getDescription(),
				issue.getStatus(),
				issue.getPriority(),
				issue.getType(),
				issue.getReporterId(),
				issue.getAssigneeId(),
				issue.getParentIssueId(),
				issue.getStoryPoints(),
				issue.getDueDate(),
				issue.getResolvedAt(),
				issue.getClosedAt(),
				issue.getVersion(),
				issue.getCreatedAt(),
				issue.getUpdatedAt(),
				labels,
				comments,
				auditLog,
				watchers);
	}

	private IssueResponse toIssueResponse(Issue issue, List<LabelResponse> labels) {
		return new IssueResponse(
				issue.getId(),
				issue.getProjectId(),
				issue.getIssueNumber(),
				issue.getTitle(),
				issue.getDescription(),
				issue.getStatus(),
				issue.getPriority(),
				issue.getType(),
				issue.getReporterId(),
				issue.getAssigneeId(),
				issue.getParentIssueId(),
				issue.getStoryPoints(),
				issue.getDueDate(),
				issue.getResolvedAt(),
				issue.getClosedAt(),
				issue.getVersion(),
				issue.getCreatedAt(),
				issue.getUpdatedAt(),
				labels);
	}
}
