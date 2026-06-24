package com.krish.issuetracker.storage;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAttachment;
import com.krish.issuetracker.exception.AttachmentNotFoundException;
import com.krish.issuetracker.exception.IssueNotFoundException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.repository.IssueAttachmentRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.storage.dto.AttachmentResponse;
import com.krish.issuetracker.storage.validation.FileTypeValidator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class AttachmentService {

	private final StorageService storageService;
	private final FileTypeValidator fileTypeValidator;
	private final IssueAttachmentRepository issueAttachmentRepository;
	private final IssueRepository issueRepository;
	private final ProjectRepository projectRepository;
	private final StorageProperties storageProperties;
	private final MeterRegistry meterRegistry;

	public AttachmentService(
			StorageService storageService,
			FileTypeValidator fileTypeValidator,
			IssueAttachmentRepository issueAttachmentRepository,
			IssueRepository issueRepository,
			ProjectRepository projectRepository,
			StorageProperties storageProperties,
			MeterRegistry meterRegistry) {
		this.storageService = storageService;
		this.fileTypeValidator = fileTypeValidator;
		this.issueAttachmentRepository = issueAttachmentRepository;
		this.issueRepository = issueRepository;
		this.projectRepository = projectRepository;
		this.storageProperties = storageProperties;
		this.meterRegistry = meterRegistry;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DEVELOPER')")
	public AttachmentResponse uploadAttachment(
			UUID orgId,
			UUID projectId,
			UUID issueId,
			MultipartFile file,
			UUID uploadedBy) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		fileTypeValidator.validate(file, storageProperties.maxFileSizeBytes());

		String fileName = filename(file);
		String storageKey = "attachments/" + issueId + "/" + UUID.randomUUID() + "/" + fileName;
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			storageService.uploadFile(file, storageKey);
		} finally {
			sample.stop(Timer.builder("storage.upload.duration").register(meterRegistry));
		}

		IssueAttachment attachment = new IssueAttachment();
		attachment.setIssueId(issueId);
		attachment.setFileName(fileName);
		attachment.setFileSizeBytes(file.getSize());
		attachment.setContentType(file.getContentType());
		attachment.setStorageKey(storageKey);
		attachment.setUploadedBy(uploadedBy);

		IssueAttachment savedAttachment = issueAttachmentRepository.save(attachment);
		log.info("Attachment uploaded: {} for issue {}", savedAttachment.getId(), issueId);
		return toAttachmentResponse(savedAttachment);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public String getPresignedDownloadUrl(UUID orgId, UUID projectId, UUID issueId, UUID attachmentId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		IssueAttachment attachment = loadAttachment(issueId, attachmentId);
		return storageService.generatePresignedUrl(attachment.getStorageKey());
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DEVELOPER')")
	public void deleteAttachment(
			UUID orgId,
			UUID projectId,
			UUID issueId,
			UUID attachmentId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		IssueAttachment attachment = loadAttachment(issueId, attachmentId);
		storageService.deleteFile(attachment.getStorageKey());
		issueAttachmentRepository.delete(attachment);
		log.info("Attachment deleted: {}", attachmentId);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public List<AttachmentResponse> listAttachments(UUID orgId, UUID projectId, UUID issueId) {
		verifyProjectAccess(orgId, projectId);
		loadIssue(projectId, issueId);
		return issueAttachmentRepository.findAllByIssueId(issueId)
				.stream()
				.map(this::toAttachmentResponse)
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

	private IssueAttachment loadAttachment(UUID issueId, UUID attachmentId) {
		return issueAttachmentRepository.findByIdAndIssueId(attachmentId, issueId)
				.orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
	}

	private String filename(MultipartFile file) {
		String filename = StringUtils.getFilename(file.getOriginalFilename());
		if (!StringUtils.hasText(filename)) {
			return "attachment";
		}
		return filename;
	}

	private AttachmentResponse toAttachmentResponse(IssueAttachment attachment) {
		return new AttachmentResponse(
				attachment.getId(),
				attachment.getIssueId(),
				attachment.getFileName(),
				attachment.getFileSizeBytes(),
				attachment.getContentType(),
				attachment.getUploadedBy(),
				attachment.getCreatedAt());
	}
}
