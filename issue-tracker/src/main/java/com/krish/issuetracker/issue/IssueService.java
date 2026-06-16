package com.krish.issuetracker.issue;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.IssueAuditLog;
import com.krish.issuetracker.domain.entity.IssueLabel;
import com.krish.issuetracker.domain.entity.IssueLabelId;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.exception.OptimisticLockException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.issue.dto.CreateIssueRequest;
import com.krish.issuetracker.issue.dto.IssueResponse;
import com.krish.issuetracker.label.dto.LabelResponse;
import com.krish.issuetracker.repository.IssueAuditLogRepository;
import com.krish.issuetracker.repository.IssueLabelRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class IssueService {

	private final IssueRepository issueRepository;
	private final IssueLabelRepository issueLabelRepository;
	private final IssueAuditLogRepository issueAuditLogRepository;
	private final ProjectRepository projectRepository;
	private final IssueNumberGenerator issueNumberGenerator;

	public IssueService(
			IssueRepository issueRepository,
			IssueLabelRepository issueLabelRepository,
			IssueAuditLogRepository issueAuditLogRepository,
			ProjectRepository projectRepository,
			IssueNumberGenerator issueNumberGenerator,
			OrganizationMemberPermissionEvaluator permissionEvaluator) {
		this.issueRepository = issueRepository;
		this.issueLabelRepository = issueLabelRepository;
		this.issueAuditLogRepository = issueAuditLogRepository;
		this.projectRepository = projectRepository;
		this.issueNumberGenerator = issueNumberGenerator;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DEVELOPER')")
	public IssueResponse createIssue(CreateIssueRequest request, UUID reporterId, UUID orgId) {
		try {
			projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(request.projectId(), orgId)
					.orElseThrow(() -> new ProjectNotFoundException(request.projectId()));

			int issueNumber = issueNumberGenerator.generateNextIssueNumber(request.projectId());

			Issue issue = new Issue();
			issue.setProjectId(request.projectId());
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
			saveIssueLabels(savedIssue, request.labelIds());
			saveCreationAuditLog(savedIssue, reporterId);

			log.info("Issue created: {} in project {}", savedIssue.getId(), request.projectId());
			return toIssueResponse(savedIssue, loadLabelResponses(savedIssue));
		} catch (ObjectOptimisticLockingFailureException ex) {
			throw new OptimisticLockException();
		}
	}

	private void saveIssueLabels(Issue issue, List<UUID> labelIds) {
		if (labelIds == null || labelIds.isEmpty()) {
			return;
		}

		List<IssueLabel> issueLabels = labelIds.stream()
				.map(labelId -> {
					IssueLabel issueLabel = new IssueLabel();
					issueLabel.setId(new IssueLabelId(issue.getId(), labelId));
					return issueLabel;
				})
				.toList();
		issueLabelRepository.saveAll(issueLabels);
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

	private List<LabelResponse> loadLabelResponses(Issue issue) {
		return issueLabelRepository.findAllByIdIssueId(issue.getId())
				.stream()
				.map(issueLabel -> new LabelResponse(
						issueLabel.getId().getLabelId(),
						issue.getProjectId(),
						null,
						null))
				.toList();
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
