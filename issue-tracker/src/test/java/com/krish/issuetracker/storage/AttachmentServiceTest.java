package com.krish.issuetracker.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAttachment;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.exception.AttachmentNotFoundException;
import com.krish.issuetracker.exception.IssueNotFoundException;
import com.krish.issuetracker.repository.IssueAttachmentRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.storage.dto.AttachmentResponse;
import com.krish.issuetracker.storage.validation.FileTypeValidator;
import com.krish.issuetracker.storage.validation.FileValidationException;
import com.krish.issuetracker.storage.validation.ValidationFailureReason;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

	@Mock
	private StorageService storageService;

	@Mock
	private FileTypeValidator fileTypeValidator;

	@Mock
	private IssueAttachmentRepository issueAttachmentRepository;

	@Mock
	private IssueRepository issueRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private StorageProperties storageProperties;

	@Spy
	private MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@InjectMocks
	private AttachmentService attachmentService;

	@Test
	void uploadAttachment_shouldSaveAttachment_whenFileIsValid() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID uploadedBy = UUID.randomUUID();
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.pdf", "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46});
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(storageProperties.maxFileSizeBytes()).thenReturn(10485760L);
		when(issueAttachmentRepository.save(any(IssueAttachment.class))).thenAnswer(invocation -> {
			IssueAttachment attachment = invocation.getArgument(0);
			attachment.setId(UUID.randomUUID());
			return attachment;
		});

		AttachmentResponse response = attachmentService.uploadAttachment(orgId, projectId, issueId, file, uploadedBy);

		assertThat(response).isNotNull();
		assertThat(response.fileName()).isEqualTo("test.pdf");
		assertThat(response.uploadedBy()).isEqualTo(uploadedBy);
		verify(storageService).uploadFile(eq(file), anyString());
	}

	@Test
	void uploadAttachment_shouldThrowFileValidationException_whenFileValidationFails() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.txt", "text/plain", "hello".getBytes());
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(storageProperties.maxFileSizeBytes()).thenReturn(10485760L);
		doThrow(new FileValidationException("File type not allowed", ValidationFailureReason.INVALID_TYPE))
				.when(fileTypeValidator).validate(file, 10485760L);

		assertThatThrownBy(() -> attachmentService.uploadAttachment(
				orgId, projectId, issueId, file, UUID.randomUUID()))
				.isInstanceOf(FileValidationException.class);
	}

	@Test
	void uploadAttachment_shouldThrowIssueNotFoundException_whenIssueDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.pdf", "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46});
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> attachmentService.uploadAttachment(
				orgId, projectId, issueId, file, UUID.randomUUID()))
				.isInstanceOf(IssueNotFoundException.class);
	}

	@Test
	void getPresignedDownloadUrl_shouldReturnUrl_whenAttachmentExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID attachmentId = UUID.randomUUID();
		IssueAttachment attachment = attachment(attachmentId, issueId, "key/path");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(issueAttachmentRepository.findByIdAndIssueId(attachmentId, issueId)).thenReturn(Optional.of(attachment));
		when(storageService.generatePresignedUrl("key/path")).thenReturn("https://example.com/presigned");

		String url = attachmentService.getPresignedDownloadUrl(orgId, projectId, issueId, attachmentId);

		assertThat(url).isEqualTo("https://example.com/presigned");
	}

	@Test
	void getPresignedDownloadUrl_shouldThrowAttachmentNotFoundException_whenAttachmentDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID attachmentId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(issueAttachmentRepository.findByIdAndIssueId(attachmentId, issueId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> attachmentService.getPresignedDownloadUrl(orgId, projectId, issueId, attachmentId))
				.isInstanceOf(AttachmentNotFoundException.class);
	}

	@Test
	void deleteAttachment_shouldDeleteAttachment_whenAttachmentExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID attachmentId = UUID.randomUUID();
		IssueAttachment attachment = attachment(attachmentId, issueId, "key/path");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(issueAttachmentRepository.findByIdAndIssueId(attachmentId, issueId)).thenReturn(Optional.of(attachment));

		attachmentService.deleteAttachment(orgId, projectId, issueId, attachmentId);

		verify(storageService).deleteFile("key/path");
		verify(issueAttachmentRepository).delete(attachment);
	}

	@Test
	void deleteAttachment_shouldThrowAttachmentNotFoundException_whenAttachmentDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		UUID attachmentId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(issueAttachmentRepository.findByIdAndIssueId(attachmentId, issueId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> attachmentService.deleteAttachment(orgId, projectId, issueId, attachmentId))
				.isInstanceOf(AttachmentNotFoundException.class);
	}

	@Test
	void listAttachments_shouldReturnAttachmentsForIssue() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID issueId = UUID.randomUUID();
		IssueAttachment attachment = attachment(UUID.randomUUID(), issueId, "key/path");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId))
				.thenReturn(Optional.of(issue(issueId, projectId)));
		when(issueAttachmentRepository.findAllByIssueId(issueId)).thenReturn(List.of(attachment));

		List<AttachmentResponse> responses = attachmentService.listAttachments(orgId, projectId, issueId);

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

	private IssueAttachment attachment(UUID attachmentId, UUID issueId, String storageKey) {
		IssueAttachment attachment = new IssueAttachment();
		attachment.setId(attachmentId);
		attachment.setIssueId(issueId);
		attachment.setFileName("test.pdf");
		attachment.setFileSizeBytes(4L);
		attachment.setContentType("application/pdf");
		attachment.setStorageKey(storageKey);
		attachment.setUploadedBy(UUID.randomUUID());
		return attachment;
	}
}
