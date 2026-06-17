package com.krish.issuetracker.label;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Label;
import com.krish.issuetracker.exception.LabelAlreadyExistsException;
import com.krish.issuetracker.exception.LabelNotFoundException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.label.dto.CreateLabelRequest;
import com.krish.issuetracker.label.dto.LabelResponse;
import com.krish.issuetracker.label.dto.UpdateLabelRequest;
import com.krish.issuetracker.repository.LabelRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LabelService {

	private final LabelRepository labelRepository;
	private final ProjectRepository projectRepository;

	public LabelService(
			LabelRepository labelRepository,
			ProjectRepository projectRepository) {
		this.labelRepository = labelRepository;
		this.projectRepository = projectRepository;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public LabelResponse createLabel(UUID orgId, UUID projectId, CreateLabelRequest request) {
		verifyProjectAccess(orgId, projectId);
		if (labelRepository.existsByNameAndProjectId(request.name(), projectId)) {
			throw new LabelAlreadyExistsException(request.name(), projectId);
		}

		Label label = new Label();
		label.setProjectId(projectId);
		label.setName(request.name());
		label.setColorHex(request.colorHex());

		Label savedLabel = labelRepository.save(label);
		log.info("Label created: {} in project {}", savedLabel.getId(), projectId);
		return toLabelResponse(savedLabel);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public LabelResponse updateLabel(UUID orgId, UUID projectId, UUID labelId, UpdateLabelRequest request) {
		verifyProjectAccess(orgId, projectId);
		Label label = loadLabel(projectId, labelId);

		if (request.name() != null && !Objects.equals(label.getName(), request.name())) {
			if (labelRepository.existsByNameAndProjectId(request.name(), projectId)) {
				throw new LabelAlreadyExistsException(request.name(), projectId);
			}
			label.setName(request.name());
		}
		if (request.colorHex() != null) {
			label.setColorHex(request.colorHex());
		}

		Label savedLabel = labelRepository.save(label);
		log.info("Label updated: {}", labelId);
		return toLabelResponse(savedLabel);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public void deleteLabel(UUID orgId, UUID projectId, UUID labelId) {
		verifyProjectAccess(orgId, projectId);
		Label label = loadLabel(projectId, labelId);
		labelRepository.delete(label);
		log.info("Label deleted: {}", labelId);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public List<LabelResponse> listLabels(UUID orgId, UUID projectId) {
		verifyProjectAccess(orgId, projectId);
		return labelRepository.findAllByProjectId(projectId)
				.stream()
				.map(this::toLabelResponse)
				.toList();
	}

	private void verifyProjectAccess(UUID orgId, UUID projectId) {
		projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private Label loadLabel(UUID projectId, UUID labelId) {
		return labelRepository.findByIdAndProjectId(labelId, projectId)
				.orElseThrow(() -> new LabelNotFoundException(labelId));
	}

	private LabelResponse toLabelResponse(Label label) {
		return new LabelResponse(
				label.getId(),
				label.getProjectId(),
				label.getName(),
				label.getColorHex());
	}
}
