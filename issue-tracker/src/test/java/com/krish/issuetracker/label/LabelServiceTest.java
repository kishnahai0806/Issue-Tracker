package com.krish.issuetracker.label;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Label;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.exception.LabelAlreadyExistsException;
import com.krish.issuetracker.exception.LabelNotFoundException;
import com.krish.issuetracker.label.dto.CreateLabelRequest;
import com.krish.issuetracker.label.dto.LabelResponse;
import com.krish.issuetracker.label.dto.UpdateLabelRequest;
import com.krish.issuetracker.repository.LabelRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

	@Mock
	private LabelRepository labelRepository;

	@Mock
	private ProjectRepository projectRepository;

	@InjectMocks
	private LabelService labelService;

	@Test
	void createLabel_shouldSaveLabel_whenNameIsUnique() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.existsByNameAndProjectId("Bug", projectId)).thenReturn(false);
		when(labelRepository.save(any(Label.class))).thenAnswer(invocation -> {
			Label label = invocation.getArgument(0);
			label.setId(UUID.randomUUID());
			return label;
		});

		LabelResponse response = labelService.createLabel(orgId, projectId, new CreateLabelRequest("Bug", "#FF5733"));

		assertThat(response).isNotNull();
		assertThat(response.name()).isEqualTo("Bug");
		assertThat(response.colorHex()).isEqualTo("#FF5733");
	}

	@Test
	void createLabel_shouldThrowLabelAlreadyExistsException_whenNameDuplicate() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.existsByNameAndProjectId("Bug", projectId)).thenReturn(true);

		assertThatThrownBy(() -> labelService.createLabel(orgId, projectId, new CreateLabelRequest("Bug", "#FF5733")))
				.isInstanceOf(LabelAlreadyExistsException.class);
	}

	@Test
	void updateLabel_shouldUpdateFields_whenLabelExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		Label label = label(labelId, projectId, "Bug", "#FF5733");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.findByIdAndProjectId(labelId, projectId)).thenReturn(Optional.of(label));
		when(labelRepository.existsByNameAndProjectId("Feature", projectId)).thenReturn(false);
		when(labelRepository.save(any(Label.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LabelResponse response = labelService.updateLabel(
				orgId, projectId, labelId, new UpdateLabelRequest("Feature", "#00FF00"));

		assertThat(response.name()).isEqualTo("Feature");
		assertThat(response.colorHex()).isEqualTo("#00FF00");
	}

	@Test
	void updateLabel_shouldThrowLabelNotFoundException_whenLabelDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.findByIdAndProjectId(labelId, projectId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> labelService.updateLabel(
				orgId, projectId, labelId, new UpdateLabelRequest("Feature", "#00FF00")))
				.isInstanceOf(LabelNotFoundException.class);
	}

	@Test
	void deleteLabel_shouldDeleteLabel_whenLabelExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		Label label = label(labelId, projectId, "Bug", "#FF5733");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.findByIdAndProjectId(labelId, projectId)).thenReturn(Optional.of(label));

		labelService.deleteLabel(orgId, projectId, labelId);

		verify(labelRepository).delete(label);
	}

	@Test
	void deleteLabel_shouldThrowLabelNotFoundException_whenLabelDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID labelId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.findByIdAndProjectId(labelId, projectId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> labelService.deleteLabel(orgId, projectId, labelId))
				.isInstanceOf(LabelNotFoundException.class);
	}

	@Test
	void listLabels_shouldReturnLabelsForProject() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		Label label = label(UUID.randomUUID(), projectId, "Bug", "#FF5733");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(labelRepository.findAllByProjectId(projectId)).thenReturn(List.of(label));

		List<LabelResponse> responses = labelService.listLabels(orgId, projectId);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).projectId()).isEqualTo(projectId);
	}

	private Project project(UUID projectId, UUID orgId) {
		Project project = new Project();
		project.setId(projectId);
		project.setOrganizationId(orgId);
		project.setName("Test Project");
		project.setKey("TEST");
		return project;
	}

	private Label label(UUID labelId, UUID projectId, String name, String colorHex) {
		Label label = new Label();
		label.setId(labelId);
		label.setProjectId(projectId);
		label.setName(name);
		label.setColorHex(colorHex);
		return label;
	}
}
