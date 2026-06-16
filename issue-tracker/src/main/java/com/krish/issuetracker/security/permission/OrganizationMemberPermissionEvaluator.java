package com.krish.issuetracker.security.permission;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.OrganizationMember;
import com.krish.issuetracker.domain.entity.OrganizationMemberId;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

@Slf4j
public class OrganizationMemberPermissionEvaluator implements PermissionEvaluator {

	/*
	 * Cache key pattern: orgmember:{orgId}:{userId}
	 * TTL: 60 seconds
	 * Eviction sites (every organization_members mutation):
	 *   1. addMember(orgId, userId)
	 *   2. removeMember(orgId, userId)
	 *   3. changeMemberRole(orgId, userId)
	 * These three eviction sites are in OrganizationService.
	 * Every one of them must call evictMembership() before this slice closes.
	 */
	private static final long CACHE_TTL_SECONDS = 60L;
	private static final String KEY_PREFIX = "orgmember:";
	private static final String ORGANIZATION_TARGET_TYPE = "ORGANIZATION";
	private static final List<String> ROLE_HIERARCHY = List.of("REPORTER", "DEVELOPER", "PROJECT_MANAGER", "ADMIN");

	private final OrganizationMemberRepository organizationMemberRepository;
	private final StringRedisTemplate redisTemplate;

	public OrganizationMemberPermissionEvaluator(
			OrganizationMemberRepository organizationMemberRepository,
			StringRedisTemplate redisTemplate) {
		this.organizationMemberRepository = organizationMemberRepository;
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		return false;
	}

	@Override
	public boolean hasPermission(
			Authentication authentication,
			Serializable targetId,
			String targetType,
			Object permission) {
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| targetId == null
				|| !ORGANIZATION_TARGET_TYPE.equals(targetType)
				|| permission == null) {
			return false;
		}

		try {
			UUID userId = UUID.fromString(authentication.getName());
			UUID organizationId = UUID.fromString(targetId.toString());
			return isMemberWithRole(userId, organizationId, permission.toString());
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private boolean isMemberWithRole(UUID userId, UUID organizationId, String requiredRole) {
		String key = membershipKey(organizationId, userId);
		String cachedRole = redisTemplate.opsForValue().get(key);

		if (cachedRole != null) {
			log.debug("Permission cache hit: {}", key);
			return roleHasPermission(cachedRole, requiredRole);
		}

		log.debug("Permission cache miss: {}", key);
		Optional<OrganizationMember> membership = organizationMemberRepository.findById(
				new OrganizationMemberId(organizationId, userId));

		if (membership.isEmpty()) {
			return false;
		}

		String memberRole = membership.get().getRole().name();
		redisTemplate.opsForValue().set(key, memberRole, Duration.ofSeconds(CACHE_TTL_SECONDS));
		return roleHasPermission(memberRole, requiredRole);
	}

	public void evictMembership(UUID organizationId, UUID userId) {
		redisTemplate.delete(membershipKey(organizationId, userId));
	}

	private boolean roleHasPermission(String memberRole, String requiredRole) {
		int memberRoleIndex = ROLE_HIERARCHY.indexOf(memberRole);
		int requiredRoleIndex = ROLE_HIERARCHY.indexOf(requiredRole);

		return memberRoleIndex >= 0 && requiredRoleIndex >= 0 && memberRoleIndex >= requiredRoleIndex;
	}

	private String membershipKey(UUID organizationId, UUID userId) {
		return KEY_PREFIX + organizationId + ":" + userId;
	}
}
