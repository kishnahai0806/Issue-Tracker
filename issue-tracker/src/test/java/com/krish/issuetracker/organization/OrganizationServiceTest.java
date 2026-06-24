package com.krish.issuetracker.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Organization;
import com.krish.issuetracker.domain.entity.OrganizationMember;
import com.krish.issuetracker.domain.entity.OrganizationMemberId;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.domain.enums.UserRole;
import com.krish.issuetracker.exception.LastOrganizationAdminException;
import com.krish.issuetracker.exception.MemberAlreadyExistsException;
import com.krish.issuetracker.exception.MemberNotFoundException;
import com.krish.issuetracker.exception.OrganizationSlugAlreadyExistsException;
import com.krish.issuetracker.organization.dto.AddMemberRequest;
import com.krish.issuetracker.organization.dto.CreateOrganizationRequest;
import com.krish.issuetracker.organization.dto.MemberResponse;
import com.krish.issuetracker.organization.dto.OrganizationResponse;
import com.krish.issuetracker.organization.dto.OrganizationSummaryResponse;
import com.krish.issuetracker.organization.dto.UpdateMemberRoleRequest;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.repository.OrganizationRepository;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private OrganizationMemberRepository organizationMemberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OrganizationMemberPermissionEvaluator permissionEvaluator;

	@InjectMocks
	private OrganizationService organizationService;

	@Test
	void createOrganization_shouldSaveOrganizationAndCreatorAsAdmin_whenSlugIsUnique() {
		UUID creatorId = UUID.randomUUID();
		when(organizationRepository.existsBySlug("acme")).thenReturn(false);
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
			Organization organization = invocation.getArgument(0);
			organization.setId(UUID.randomUUID());
			return organization;
		});
		when(organizationMemberRepository.save(any(OrganizationMember.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		OrganizationResponse response = organizationService.createOrganization(
				new CreateOrganizationRequest("Acme", "acme"), creatorId);

		assertThat(response).isNotNull();
		assertThat(response.slug()).isEqualTo("acme");
		assertThat(response.plan()).isEqualTo("FREE");
	}

	@Test
	void createOrganization_shouldThrowOrganizationSlugAlreadyExistsException_whenSlugDuplicate() {
		when(organizationRepository.existsBySlug("acme")).thenReturn(true);

		assertThatThrownBy(() -> organizationService.createOrganization(
				new CreateOrganizationRequest("Acme", "acme"), UUID.randomUUID()))
				.isInstanceOf(OrganizationSlugAlreadyExistsException.class);
	}

	@Test
	void listOrganizations_shouldReturnOnlyOrgsWhereUserIsMember() {
		UUID userId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		Organization organization = organization(orgId, "acme");
		when(organizationMemberRepository.findOrganizationIdsByUserId(userId)).thenReturn(List.of(orgId));
		when(organizationRepository.findAllByIdInAndIsActiveTrue(List.of(orgId))).thenReturn(List.of(organization));

		List<OrganizationSummaryResponse> responses = organizationService.listOrganizations(userId);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).id()).isEqualTo(orgId);
	}

	@Test
	void addMember_shouldAddMember_whenNotAlreadyMember() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		OrganizationMemberId memberId = new OrganizationMemberId(orgId, userId);
		User user = user(userId, "member@test.com", "Member Name");
		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization(orgId, "acme")));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(organizationMemberRepository.existsById(memberId)).thenReturn(false);
		when(organizationMemberRepository.save(any(OrganizationMember.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		MemberResponse response = organizationService.addMember(
				orgId, new AddMemberRequest(userId, UserRole.DEVELOPER));

		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.role()).isEqualTo(UserRole.DEVELOPER);
	}

	@Test
	void addMember_shouldThrowMemberAlreadyExistsException_whenAlreadyMember() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		OrganizationMemberId memberId = new OrganizationMemberId(orgId, userId);
		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization(orgId, "acme")));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "member@test.com", "Member Name")));
		when(organizationMemberRepository.existsById(memberId)).thenReturn(true);

		assertThatThrownBy(() -> organizationService.addMember(
				orgId, new AddMemberRequest(userId, UserRole.DEVELOPER)))
				.isInstanceOf(MemberAlreadyExistsException.class);
	}

	@Test
	void removeMember_shouldRemoveMember_whenNotLastAdmin() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		OrganizationMember member = member(orgId, userId, UserRole.DEVELOPER);
		when(organizationMemberRepository.findById(new OrganizationMemberId(orgId, userId)))
				.thenReturn(Optional.of(member));

		organizationService.removeMember(orgId, userId);

		verify(organizationMemberRepository).delete(member);
	}

	@Test
	void removeMember_shouldThrowLastOrganizationAdminException_whenRemovingLastAdmin() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		OrganizationMember member = member(orgId, userId, UserRole.ADMIN);
		when(organizationMemberRepository.findById(new OrganizationMemberId(orgId, userId)))
				.thenReturn(Optional.of(member));
		when(organizationMemberRepository.countById_OrganizationIdAndRole(orgId, UserRole.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> organizationService.removeMember(orgId, userId))
				.isInstanceOf(LastOrganizationAdminException.class);
	}

	@Test
	void updateMemberRole_shouldUpdateRole_whenMemberExists() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		OrganizationMember member = member(orgId, userId, UserRole.DEVELOPER);
		when(organizationMemberRepository.findById(new OrganizationMemberId(orgId, userId)))
				.thenReturn(Optional.of(member));
		when(organizationMemberRepository.save(any(OrganizationMember.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "member@test.com", "Member Name")));

		MemberResponse response = organizationService.updateMemberRole(
				orgId, userId, new UpdateMemberRoleRequest(UserRole.PROJECT_MANAGER));

		assertThat(response.role()).isEqualTo(UserRole.PROJECT_MANAGER);
	}

	@Test
	void updateMemberRole_shouldThrowMemberNotFoundException_whenMemberDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(organizationMemberRepository.findById(new OrganizationMemberId(orgId, userId)))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> organizationService.updateMemberRole(
				orgId, userId, new UpdateMemberRoleRequest(UserRole.PROJECT_MANAGER)))
				.isInstanceOf(MemberNotFoundException.class);
	}

	private Organization organization(UUID id, String slug) {
		Organization organization = new Organization();
		organization.setId(id);
		organization.setName("Acme");
		organization.setSlug(slug);
		organization.setPlan("FREE");
		organization.setActive(true);
		return organization;
	}

	private User user(UUID id, String email, String fullName) {
		User user = new User();
		user.setId(id);
		user.setEmail(email);
		user.setFullName(fullName);
		return user;
	}

	private OrganizationMember member(UUID orgId, UUID userId, UserRole role) {
		OrganizationMember member = new OrganizationMember();
		member.setId(new OrganizationMemberId(orgId, userId));
		member.setRole(role);
		return member;
	}
}
