package com.krish.issuetracker.organization;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Organization;
import com.krish.issuetracker.domain.entity.OrganizationMember;
import com.krish.issuetracker.domain.entity.OrganizationMemberId;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.domain.enums.UserRole;
import com.krish.issuetracker.exception.MemberAlreadyExistsException;
import com.krish.issuetracker.exception.MemberNotFoundException;
import com.krish.issuetracker.exception.LastOrganizationAdminException;
import com.krish.issuetracker.exception.OrganizationNotFoundException;
import com.krish.issuetracker.exception.OrganizationSlugAlreadyExistsException;
import com.krish.issuetracker.exception.UserNotFoundException;
import com.krish.issuetracker.organization.dto.AddMemberRequest;
import com.krish.issuetracker.organization.dto.CreateOrganizationRequest;
import com.krish.issuetracker.organization.dto.MemberResponse;
import com.krish.issuetracker.organization.dto.OrganizationResponse;
import com.krish.issuetracker.organization.dto.OrganizationSummaryResponse;
import com.krish.issuetracker.organization.dto.UpdateMemberRoleRequest;
import com.krish.issuetracker.organization.dto.UpdateOrganizationRequest;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.repository.OrganizationRepository;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class OrganizationService {

	private static final String DEFAULT_PLAN = "FREE";

	private final OrganizationRepository organizationRepository;
	private final OrganizationMemberRepository organizationMemberRepository;
	private final UserRepository userRepository;
	private final OrganizationMemberPermissionEvaluator permissionEvaluator;

	public OrganizationService(
			OrganizationRepository organizationRepository,
			OrganizationMemberRepository organizationMemberRepository,
			UserRepository userRepository,
			OrganizationMemberPermissionEvaluator permissionEvaluator) {
		this.organizationRepository = organizationRepository;
		this.organizationMemberRepository = organizationMemberRepository;
		this.userRepository = userRepository;
		this.permissionEvaluator = permissionEvaluator;
	}

	@Transactional
	@PreAuthorize("isAuthenticated()")
	public OrganizationResponse createOrganization(CreateOrganizationRequest request, UUID creatorUserId) {
		if (organizationRepository.existsBySlug(request.slug())) {
			throw new OrganizationSlugAlreadyExistsException(request.slug());
		}

		Organization organization = new Organization();
		organization.setName(request.name());
		organization.setSlug(request.slug());
		organization.setPlan(DEFAULT_PLAN);
		organization.setActive(true);

		Organization savedOrganization = organizationRepository.save(organization);

		OrganizationMember creatorMembership = new OrganizationMember();
		creatorMembership.setId(new OrganizationMemberId(savedOrganization.getId(), creatorUserId));
		creatorMembership.setRole(UserRole.ADMIN);
		organizationMemberRepository.save(creatorMembership);

		log.info("Organization created: {}", savedOrganization.getId());
		return toOrganizationResponse(savedOrganization);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public OrganizationResponse getOrganization(UUID orgId) {
		return toOrganizationResponse(loadOrganization(orgId));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("isAuthenticated()")
	public List<OrganizationSummaryResponse> listOrganizations(UUID requestingUserId) {
		List<UUID> organizationIds = organizationMemberRepository.findOrganizationIdsByUserId(requestingUserId);
		if (organizationIds.isEmpty()) {
			return List.of();
		}

		return organizationRepository.findAllByIdInAndIsActiveTrue(organizationIds)
				.stream()
				.map(this::toOrganizationSummaryResponse)
				.toList();
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'ADMIN')")
	public OrganizationResponse updateOrganization(
			UUID orgId,
			UpdateOrganizationRequest request,
			UUID requestingUserId) {
		Organization organization = loadOrganization(orgId);

		if (request.name() != null) {
			organization.setName(request.name());
		}

		Organization savedOrganization = organizationRepository.save(organization);
		return toOrganizationResponse(savedOrganization);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'ADMIN')")
	public MemberResponse addMember(UUID orgId, AddMemberRequest request, UUID requestingUserId) {
		loadOrganization(orgId);
		User user = loadUser(request.userId());
		OrganizationMemberId memberId = new OrganizationMemberId(orgId, request.userId());

		if (organizationMemberRepository.existsById(memberId)) {
			throw new MemberAlreadyExistsException(request.userId(), orgId);
		}

		OrganizationMember member = new OrganizationMember();
		member.setId(memberId);
		member.setRole(request.role());

		OrganizationMember savedMember = organizationMemberRepository.save(member);
		permissionEvaluator.evictMembership(orgId, request.userId()); // EVICTION SITE 1
		log.info("Member added to org {}: {}", orgId, request.userId());

		return toMemberResponse(savedMember, user);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'ADMIN')")
	public void removeMember(UUID orgId, UUID userId, UUID requestingUserId) {
		OrganizationMember member = loadMember(orgId, userId);
		preventRemovingLastAdmin(orgId, member);

		organizationMemberRepository.delete(member);
		permissionEvaluator.evictMembership(orgId, userId); // EVICTION SITE 2
		log.info("Member removed from org {}: {}", orgId, userId);
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'ADMIN')")
	public MemberResponse updateMemberRole(
			UUID orgId,
			UUID userId,
			UpdateMemberRoleRequest request,
			UUID requestingUserId) {
		OrganizationMember member = loadMember(orgId, userId);
		preventDemotingLastAdmin(orgId, member, request.role());

		member.setRole(request.role());
		OrganizationMember savedMember = organizationMemberRepository.save(member);
		permissionEvaluator.evictMembership(orgId, userId); // EVICTION SITE 3
		log.info("Member role updated in org {}: {}", orgId, userId);

		return toMemberResponse(savedMember, loadUser(userId));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public List<MemberResponse> listMembers(UUID orgId) {
		loadOrganization(orgId);
		return organizationMemberRepository.findById_OrganizationId(orgId)
				.stream()
				.map(member -> toMemberResponse(member, loadUser(member.getId().getUserId())))
				.toList();
	}

	private Organization loadOrganization(UUID orgId) {
		return organizationRepository.findById(orgId)
				.orElseThrow(() -> new OrganizationNotFoundException(orgId));
	}

	private User loadUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
	}

	private OrganizationMember loadMember(UUID orgId, UUID userId) {
		return organizationMemberRepository.findById(new OrganizationMemberId(orgId, userId))
				.orElseThrow(() -> new MemberNotFoundException(userId, orgId));
	}

	private void preventRemovingLastAdmin(UUID orgId, OrganizationMember member) {
		if (member.getRole() == UserRole.ADMIN && adminCount(orgId) == 1) {
			throw new LastOrganizationAdminException("Cannot remove the last admin of an organization");
		}
	}

	private void preventDemotingLastAdmin(UUID orgId, OrganizationMember member, UserRole newRole) {
		if (member.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN && adminCount(orgId) == 1) {
			throw new LastOrganizationAdminException("Cannot demote the last admin of an organization");
		}
	}

	private long adminCount(UUID orgId) {
		return organizationMemberRepository.countById_OrganizationIdAndRole(orgId, UserRole.ADMIN);
	}

	private OrganizationResponse toOrganizationResponse(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getSlug(),
				organization.getPlan(),
				organization.isActive(),
				organization.getCreatedAt());
	}

	private OrganizationSummaryResponse toOrganizationSummaryResponse(Organization organization) {
		return new OrganizationSummaryResponse(
				organization.getId(),
				organization.getName(),
				organization.getSlug(),
				organization.getPlan());
	}

	private MemberResponse toMemberResponse(OrganizationMember member, User user) {
		return new MemberResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				member.getRole(),
				member.getJoinedAt());
	}
}
