package com.krish.issuetracker.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAuditLog;
import com.krish.issuetracker.domain.entity.IssueComment;
import com.krish.issuetracker.domain.entity.IssueWatcher;
import com.krish.issuetracker.domain.entity.IssueWatcherId;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.exception.AccessDeniedException;
import com.krish.issuetracker.exception.IssueNotFoundException;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	@Mock
	private IssueCommentRepository issueCommentRepository;

	@Mock
	private IssueAuditLogRepository issueAuditLogRepository;

	@Mock
	private IssueRepository issueRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private IssueWatcherRepository issueWatcherRepository;

	@Mock
	private OrganizationMemberRepository organizationMemberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationEventPublisher notificationEventPublisher;

	@Spy
	private MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@InjectMocks
	private CommentService commentService;

	@Test
	void addComment_shouldSaveCommentAndSetAuthor_whenIssueExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.save(any(IssueComment.class))).thenAnswer(invocation -> {
			IssueComment comment = invocation.getArgument(0);
			comment.setId(UUID.randomUUID());
			return comment;
		});
		when(issueWatcherRepository.findAllByIdIssueId(issueId)).thenReturn(List.of());

		CommentResponse response = commentService.addComment(
				orgId, projectId, issueId, new CreateCommentRequest("A new comment"), authorId);

		assertThat(response).isNotNull();
		assertThat(response.authorId()).isEqualTo(authorId);
		assertThat(response.content()).isEqualTo("A new comment");
		verify(issueCommentRepository).save(any(IssueComment.class));
	}

	@Test
	void addComment_shouldThrowIssueNotFoundException_whenIssueDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.addComment(
				orgId, projectId, issueId, new CreateCommentRequest("content"), UUID.randomUUID()))
				.isInstanceOf(IssueNotFoundException.class);
	}

	@Test
	void addComment_shouldNotNotifyWatcher_whenWatcherIsNoLongerOrgMember() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		UUID watcherId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueWatcher watcher = new IssueWatcher();
		watcher.setId(new IssueWatcherId(issueId, watcherId));
		User watcherUser = new User();
		watcherUser.setId(watcherId);
		watcherUser.setEmail("watcher@example.com");
		watcherUser.setFullName("Watcher Name");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.save(any(IssueComment.class))).thenAnswer(invocation -> {
			IssueComment comment = invocation.getArgument(0);
			comment.setId(UUID.randomUUID());
			return comment;
		});
		when(issueWatcherRepository.findAllByIdIssueId(issueId)).thenReturn(List.of(watcher));
		when(organizationMemberRepository.existsById_OrganizationIdAndId_UserId(orgId, watcherId)).thenReturn(false);

		commentService.addComment(orgId, projectId, issueId, new CreateCommentRequest("A new comment"), authorId);

		verify(userRepository, never()).findByIdAndIsActiveTrue(watcherId);
		verify(notificationEventPublisher, never()).publishEmailNotification(any(), any(), any(), any(), any());
	}

	@Test
	void updateComment_shouldUpdateContent_whenAuthorMatches() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueComment comment = comment(commentId, issueId, authorId, "old content");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.findByIdAndIssueId(commentId, issueId)).thenReturn(Optional.of(comment));
		when(issueCommentRepository.save(any(IssueComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CommentResponse response = commentService.updateComment(
				orgId, projectId, issueId, commentId, new UpdateCommentRequest("updated content"), authorId);

		assertThat(response.content()).isEqualTo("updated content");
		assertThat(response.isEdited()).isTrue();
		ArgumentCaptor<IssueAuditLog> auditLogCaptor = ArgumentCaptor.forClass(IssueAuditLog.class);
		verify(issueAuditLogRepository).save(auditLogCaptor.capture());
		assertThat(auditLogCaptor.getValue().getIssueId()).isEqualTo(issueId);
		assertThat(auditLogCaptor.getValue().getChangedBy()).isEqualTo(authorId);
		assertThat(auditLogCaptor.getValue().getFieldName()).isEqualTo("comment_updated");
		assertThat(auditLogCaptor.getValue().getOldValue()).isEqualTo("old content");
		assertThat(auditLogCaptor.getValue().getNewValue()).isEqualTo("updated content");
	}

	@Test
	void updateComment_shouldThrowAccessDeniedException_whenAuthorDoesNotMatch() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueComment comment = comment(commentId, issueId, UUID.randomUUID(), "old content");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.findByIdAndIssueId(commentId, issueId)).thenReturn(Optional.of(comment));

		assertThatThrownBy(() -> commentService.updateComment(
				orgId, projectId, issueId, commentId, new UpdateCommentRequest("updated content"), UUID.randomUUID()))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void deleteComment_shouldDeleteComment_whenAuthorMatches() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueComment comment = comment(commentId, issueId, authorId, "content");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.findByIdAndIssueId(commentId, issueId)).thenReturn(Optional.of(comment));

		commentService.deleteComment(orgId, projectId, issueId, commentId, authorId);

		verify(issueCommentRepository).delete(comment);
		ArgumentCaptor<IssueAuditLog> auditLogCaptor = ArgumentCaptor.forClass(IssueAuditLog.class);
		verify(issueAuditLogRepository).save(auditLogCaptor.capture());
		assertThat(auditLogCaptor.getValue().getIssueId()).isEqualTo(issueId);
		assertThat(auditLogCaptor.getValue().getChangedBy()).isEqualTo(authorId);
		assertThat(auditLogCaptor.getValue().getFieldName()).isEqualTo("comment_deleted");
		assertThat(auditLogCaptor.getValue().getOldValue()).isEqualTo(commentId.toString());
		assertThat(auditLogCaptor.getValue().getNewValue()).isNull();
	}

	@Test
	void deleteComment_shouldThrowAccessDeniedException_whenAuthorDoesNotMatch() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueComment comment = comment(commentId, issueId, UUID.randomUUID(), "content");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.findByIdAndIssueId(commentId, issueId)).thenReturn(Optional.of(comment));

		assertThatThrownBy(() -> commentService.deleteComment(orgId, projectId, issueId, commentId, UUID.randomUUID()))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void listComments_shouldReturnCommentsForCorrectIssue() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		Project project = project(projectId, orgId);
		Issue issue = issue(issueId, projectId);
		IssueComment comment = comment(UUID.randomUUID(), issueId, UUID.randomUUID(), "content");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue));
		when(issueCommentRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId)).thenReturn(List.of(comment));

		List<CommentResponse> responses = commentService.getComments(orgId, projectId, issueId);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).issueId()).isEqualTo(issueId);
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
		return issue;
	}

	private IssueComment comment(UUID commentId, UUID issueId, UUID authorId, String content) {
		IssueComment comment = new IssueComment();
		comment.setId(commentId);
		comment.setIssueId(issueId);
		comment.setAuthorId(authorId);
		comment.setContent(content);
		return comment;
	}
}
