package com.krish.issuetracker.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueComment;
import com.krish.issuetracker.domain.entity.IssueLabel;
import com.krish.issuetracker.domain.entity.IssueLabelId;
import com.krish.issuetracker.domain.entity.IssueWatcher;
import com.krish.issuetracker.domain.entity.IssueWatcherId;
import com.krish.issuetracker.domain.entity.Label;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import com.krish.issuetracker.exception.AccessDeniedException;
import com.krish.issuetracker.exception.AssigneeNotMemberException;
import com.krish.issuetracker.exception.IssueNotFoundException;
import com.krish.issuetracker.exception.LabelNotInProjectException;
import com.krish.issuetracker.exception.OptimisticLockException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.exception.WatcherNotMemberException;
import com.krish.issuetracker.issue.dto.CreateIssueRequest;
import com.krish.issuetracker.issue.dto.IssueDetailResponse;
import com.krish.issuetracker.issue.dto.IssueResponse;
import com.krish.issuetracker.issue.dto.UpdateIssueRequest;
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
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import com.krish.issuetracker.websocket.WebSocketEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

	@Mock
	private IssueRepository issueRepository;

	@Mock
	private IssueCommentRepository issueCommentRepository;

	@Mock
	private IssueLabelRepository issueLabelRepository;

	@Mock
	private IssueWatcherRepository issueWatcherRepository;

	@Mock
	private IssueAuditLogRepository issueAuditLogRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OrganizationMemberRepository organizationMemberRepository;

	@Mock
	private OrganizationMemberPermissionEvaluator permissionEvaluator;

	@Mock
	private LabelRepository labelRepository;

	@Mock
	private IssueNumberGenerator issueNumberGenerator;

	@Mock
	private WebSocketEventPublisher eventPublisher;

	@Mock
	private NotificationEventPublisher notificationEventPublisher;

	@Spy
	private MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@InjectMocks
	private IssueService issueService;

	@Test
	void createIssue_shouldCreateIssue_whenRequestIsValid() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID reporterId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueNumberGenerator.generateNextIssueNumber(projectId)).thenReturn(1);
		when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
			Issue issue = invocation.getArgument(0);
			issue.setId(UUID.randomUUID());
			return issue;
		});

		IssueResponse response = issueService.createIssue(
				orgId, projectId, createIssueRequest("New issue", null, null, List.of()), reporterId);

		assertThat(response).isNotNull();
		assertThat(response.title()).isEqualTo("New issue");
		assertThat(response.issueNumber()).isEqualTo(1);
		assertThat(response.reporterId()).isEqualTo(reporterId);
	}

	@Test
	void createIssue_shouldThrowProjectNotFoundException_whenProjectDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.createIssue(
				orgId, projectId, createIssueRequest("New issue", null, null, List.of()), UUID.randomUUID()))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void createIssue_shouldThrowAssigneeNotMemberException_whenAssigneeNotOrgMember() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID assigneeId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, assigneeId)).thenReturn(false);

		assertThatThrownBy(() -> issueService.createIssue(
				orgId, projectId, createIssueRequest("New issue", assigneeId, null, List.of()), UUID.randomUUID()))
				.isInstanceOf(AssigneeNotMemberException.class);
	}

	@Test
	void createIssue_shouldThrowLabelNotInProjectException_whenLabelNotInProject() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueNumberGenerator.generateNextIssueNumber(projectId)).thenReturn(1);
		when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
			Issue issue = invocation.getArgument(0);
			issue.setId(UUID.randomUUID());
			return issue;
		});
		when(labelRepository.findAllById(List.of(labelId))).thenReturn(List.of());

		assertThatThrownBy(() -> issueService.createIssue(
				orgId, projectId, createIssueRequest("New issue", null, null, List.of(labelId)), UUID.randomUUID()))
				.isInstanceOf(LabelNotInProjectException.class);
	}

	@Test
	void createIssue_shouldAssignLabelsAndValidateAssigneeAndParentIssue_whenAllValid() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID reporterId = UUID.randomUUID();
		UUID assigneeId = UUID.randomUUID();
		UUID parentIssueId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue parentIssue = issue(parentIssueId, projectId);
		Label label = new Label();
		label.setId(labelId);
		label.setProjectId(projectId);
		label.setName("Bug");
		label.setColorHex("#FF0000");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, assigneeId)).thenReturn(true);
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(parentIssueId, projectId))
				.thenReturn(Optional.of(parentIssue));
		when(issueNumberGenerator.generateNextIssueNumber(projectId)).thenReturn(2);
		when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
			Issue issue = invocation.getArgument(0);
			issue.setId(UUID.randomUUID());
			return issue;
		});
		when(labelRepository.findAllById(List.of(labelId))).thenReturn(List.of(label));

		IssueResponse response = issueService.createIssue(
				orgId, projectId, createIssueRequest("New issue", assigneeId, parentIssueId, List.of(labelId)), reporterId);

		assertThat(response.assigneeId()).isEqualTo(assigneeId);
		assertThat(response.parentIssueId()).isEqualTo(parentIssueId);
		verify(issueLabelRepository).saveAll(any());
	}

	@Test
	void createIssue_shouldThrowOptimisticLockException_whenSaveConflicts() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueNumberGenerator.generateNextIssueNumber(projectId)).thenReturn(1);
		when(issueRepository.save(any(Issue.class)))
				.thenThrow(new ObjectOptimisticLockingFailureException(Issue.class, "id"));

		assertThatThrownBy(() -> issueService.createIssue(
				orgId, projectId, createIssueRequest("New issue", null, null, List.of()), UUID.randomUUID()))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void getIssue_shouldReturnIssueDetail_whenIssueExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueComment comment = new IssueComment();
		comment.setId(UUID.randomUUID());
		comment.setIssueId(issueId);
		comment.setAuthorId(UUID.randomUUID());
		comment.setContent("A comment");
		IssueLabel issueLabel = new IssueLabel();
		issueLabel.setId(new IssueLabelId(issueId, labelId));
		Label label = new Label();
		label.setId(labelId);
		label.setProjectId(projectId);
		label.setName("Bug");
		label.setColorHex("#FF0000");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId)).thenReturn(List.of(comment));
		when(issueLabelRepository.findAllByIdIssueId(issueId)).thenReturn(List.of(issueLabel));
		when(labelRepository.findAllById(List.of(labelId))).thenReturn(List.of(label));

		IssueDetailResponse response = issueService.getIssue(orgId, projectId, issueId);

		assertThat(response.id()).isEqualTo(issueId);
		assertThat(response.title()).isEqualTo(issue.getTitle());
		assertThat(response.comments()).hasSize(1);
		assertThat(response.labels()).hasSize(1);
	}

	@Test
	void getIssue_shouldThrowIssueNotFoundException_whenIssueDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.getIssue(orgId, projectId, issueId))
				.isInstanceOf(IssueNotFoundException.class);
	}

	@Test
	void updateIssue_shouldUpdateStatus_whenVersionMatches() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		issue.setStatus(IssueStatus.TODO);
		issue.setVersion(0L);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IssueResponse response = issueService.updateIssue(
				orgId, projectId, issueId, updateIssueRequest(IssueStatus.DONE, 0L), UUID.randomUUID());

		assertThat(response.status()).isEqualTo(IssueStatus.DONE);
	}

	@Test
	void updateIssue_shouldThrowOptimisticLockException_whenVersionMismatch() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		issue.setVersion(5L);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));

		assertThatThrownBy(() -> issueService.updateIssue(
				orgId, projectId, issueId, updateIssueRequest(IssueStatus.DONE, 1L), UUID.randomUUID()))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void updateIssue_shouldThrowOptimisticLockException_whenSaveConflicts() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		issue.setVersion(0L);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueRepository.saveAndFlush(any(Issue.class)))
				.thenThrow(new ObjectOptimisticLockingFailureException(Issue.class, issueId.toString()));

		assertThatThrownBy(() -> issueService.updateIssue(
				orgId, projectId, issueId, updateIssueRequest(IssueStatus.DONE, 0L), UUID.randomUUID()))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void updateIssue_shouldUpdateAllFieldsAndNotifyAssigneeAndWatchers_whenAssigneeAndStatusChangeToClosed() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID newAssigneeId = UUID.randomUUID();
		UUID newParentIssueId = UUID.randomUUID();
		UUID watcherId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		issue.setStatus(IssueStatus.TODO);
		issue.setVersion(0L);
		Issue newParentIssue = issue(newParentIssueId, projectId);
		User assigneeUser = new User();
		assigneeUser.setId(newAssigneeId);
		assigneeUser.setEmail("assignee@example.com");
		assigneeUser.setFullName("Assignee Name");
		User watcherUser = new User();
		watcherUser.setId(watcherId);
		watcherUser.setEmail("watcher@example.com");
		watcherUser.setFullName("Watcher Name");
		IssueWatcher watcher = new IssueWatcher();
		watcher.setId(new IssueWatcherId(issueId, watcherId));
		UpdateIssueRequest request = new UpdateIssueRequest(
				"New title",
				"New description",
				IssueStatus.CLOSED,
				IssuePriority.HIGH,
				IssueType.BUG,
				newAssigneeId,
				newParentIssueId,
				5,
				LocalDate.of(2026, 1, 1),
				0L);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, newAssigneeId)).thenReturn(true);
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(newParentIssueId, projectId))
				.thenReturn(Optional.of(newParentIssue));
		when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.findByIdAndIsActiveTrue(newAssigneeId)).thenReturn(Optional.of(assigneeUser));
		when(issueWatcherRepository.findAllByIdIssueId(issueId)).thenReturn(List.of(watcher));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, watcherId)).thenReturn(true);
		when(userRepository.findByIdAndIsActiveTrue(watcherId)).thenReturn(Optional.of(watcherUser));

		IssueResponse response = issueService.updateIssue(orgId, projectId, issueId, request, UUID.randomUUID());

		assertThat(response.title()).isEqualTo("New title");
		assertThat(response.description()).isEqualTo("New description");
		assertThat(response.status()).isEqualTo(IssueStatus.CLOSED);
		assertThat(response.priority()).isEqualTo(IssuePriority.HIGH);
		assertThat(response.type()).isEqualTo(IssueType.BUG);
		assertThat(response.assigneeId()).isEqualTo(newAssigneeId);
		assertThat(response.parentIssueId()).isEqualTo(newParentIssueId);
		assertThat(response.storyPoints()).isEqualTo(5);
		assertThat(response.resolvedAt()).isNotNull();
		assertThat(response.closedAt()).isNotNull();
		verify(notificationEventPublisher, times(2))
				.publishEmailNotification(any(), any(), any(), any(), any());
	}

	@Test
	void updateIssue_shouldClearLifecycleTimestamps_whenReopenedFromClosed() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		issue.setStatus(IssueStatus.CLOSED);
		issue.setResolvedAt(LocalDateTime.now().minusDays(2));
		issue.setClosedAt(LocalDateTime.now().minusDays(1));
		issue.setVersion(0L);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(issueWatcherRepository.findAllByIdIssueId(issueId)).thenReturn(List.of());

		IssueResponse response = issueService.updateIssue(
				orgId,
				projectId,
				issueId,
				updateIssueRequest(IssueStatus.TODO, 0L),
				UUID.randomUUID());

		assertThat(response.status()).isEqualTo(IssueStatus.TODO);
		assertThat(response.resolvedAt()).isNull();
		assertThat(response.closedAt()).isNull();
	}

	@Test
	void deleteIssue_shouldSoftDeleteIssue_whenIssueExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);

		issueService.deleteIssue(orgId, projectId, issueId, UUID.randomUUID());

		verify(issueRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getDeletedAt()).isNotNull();
	}

	@Test
	void deleteIssue_shouldThrowOptimisticLockException_whenSaveConflicts() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueRepository.saveAndFlush(any(Issue.class)))
				.thenThrow(new ObjectOptimisticLockingFailureException(Issue.class, issueId.toString()));

		assertThatThrownBy(() -> issueService.deleteIssue(orgId, projectId, issueId, UUID.randomUUID()))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void addWatcher_shouldAddWatcher_whenUserIsOrgMember() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, userId)).thenReturn(true);
		when(issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, userId)).thenReturn(false);

		issueService.addWatcher(orgId, projectId, issueId, userId, userId);

		verify(issueWatcherRepository).save(any(IssueWatcher.class));
	}

	@Test
	void addWatcher_shouldReturnEarly_whenUserAlreadyWatching() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, userId)).thenReturn(true);
		when(issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, userId)).thenReturn(true);

		issueService.addWatcher(orgId, projectId, issueId, userId, userId);

		verify(issueWatcherRepository, never()).save(any(IssueWatcher.class));
	}

	@Test
	void addWatcher_shouldThrowWatcherNotMemberException_whenUserNotOrgMember() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, userId)).thenReturn(false);

		assertThatThrownBy(() -> issueService.addWatcher(orgId, projectId, issueId, userId, userId))
				.isInstanceOf(WatcherNotMemberException.class);
	}

	@Test
	void addWatcher_shouldThrowAccessDeniedException_whenReporterAddsAnotherUser() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID targetUserId = UUID.randomUUID();
		UUID requestingUserId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(permissionEvaluator.hasPermission(any(), eq(orgId), eq("ORGANIZATION"), eq("DEVELOPER")))
				.thenReturn(false);

		assertThatThrownBy(() -> issueService.addWatcher(orgId, projectId, issueId, targetUserId, requestingUserId))
				.isInstanceOf(AccessDeniedException.class);

		verify(issueWatcherRepository, never()).save(any(IssueWatcher.class));
	}

	@Test
	void addWatcher_shouldAddWatcher_whenElevatedUserAddsAnotherUser() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID targetUserId = UUID.randomUUID();
		UUID requestingUserId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(permissionEvaluator.hasPermission(any(), eq(orgId), eq("ORGANIZATION"), eq("DEVELOPER")))
				.thenReturn(true);
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, targetUserId)).thenReturn(true);
		when(issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, targetUserId)).thenReturn(false);

		issueService.addWatcher(orgId, projectId, issueId, targetUserId, requestingUserId);

		verify(issueWatcherRepository).save(any(IssueWatcher.class));
	}

	@Test
	void removeWatcher_shouldRemoveWatcher_whenIssueExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));

		issueService.removeWatcher(orgId, projectId, issueId, userId, userId);

		verify(issueWatcherRepository).deleteByIdIssueIdAndIdUserId(issueId, userId);
	}

	@Test
	void updateIssue_shouldNotNotifyWatcher_whenWatcherIsNoLongerOrgMember() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID watcherId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		issue.setStatus(IssueStatus.TODO);
		issue.setVersion(0L);
		IssueWatcher watcher = new IssueWatcher();
		watcher.setId(new IssueWatcherId(issueId, watcherId));
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(issueWatcherRepository.findAllByIdIssueId(issueId)).thenReturn(List.of(watcher));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, watcherId)).thenReturn(false);

		issueService.updateIssue(
				orgId,
				projectId,
				issueId,
				updateIssueRequest(IssueStatus.DONE, 0L),
				UUID.randomUUID());

		verify(userRepository, never()).findByIdAndIsActiveTrue(watcherId);
		verify(notificationEventPublisher, never()).publishEmailNotification(any(), any(), any(), any(), any());
	}

	private Project project(UUID projectId, UUID orgId) {
		Project project = new Project();
		project.setId(projectId);
		project.setOrganizationId(orgId);
		project.setName("Test Project");
		project.setKey("TEST");
		return project;
	}

	private Issue issue(UUID issueId, UUID projectId) {
		Issue issue = new Issue();
		issue.setId(issueId);
		issue.setProjectId(projectId);
		issue.setTitle("Test issue");
		issue.setIssueNumber(1);
		issue.setStatus(IssueStatus.TODO);
		issue.setPriority(IssuePriority.MEDIUM);
		issue.setType(IssueType.TASK);
		issue.setReporterId(UUID.randomUUID());
		issue.setVersion(0L);
		return issue;
	}

	private CreateIssueRequest createIssueRequest(String title, UUID assigneeId, UUID parentIssueId, List<UUID> labelIds) {
		return new CreateIssueRequest(
				title,
				"description",
				IssueStatus.TODO,
				IssuePriority.MEDIUM,
				IssueType.TASK,
				assigneeId,
				parentIssueId,
				null,
				null,
				labelIds);
	}

	private UpdateIssueRequest updateIssueRequest(IssueStatus status, Long version) {
		return new UpdateIssueRequest(null, null, status, null, null, null, null, null, null, version);
	}
}
