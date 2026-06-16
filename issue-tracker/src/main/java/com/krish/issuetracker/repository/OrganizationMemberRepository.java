package com.krish.issuetracker.repository;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.OrganizationMember;
import com.krish.issuetracker.domain.entity.OrganizationMemberId;
import com.krish.issuetracker.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, OrganizationMemberId> {

	List<OrganizationMember> findById_UserId(UUID userId);

	List<OrganizationMember> findById_OrganizationId(UUID organizationId);

	long countById_OrganizationIdAndRole(UUID organizationId, UserRole role);

	boolean existsById_OrganizationIdAndId_UserId(UUID organizationId, UUID userId);
}
