package com.krish.issuetracker.repository;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.OrganizationMember;
import com.krish.issuetracker.domain.entity.OrganizationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, OrganizationMemberId> {

	List<OrganizationMember> findById_UserId(UUID userId);

	boolean existsById_OrganizationIdAndId_UserId(UUID organizationId, UUID userId);
}
