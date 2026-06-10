package com.krish.issuetracker.domain.entity;

import java.time.LocalDateTime;

import com.krish.issuetracker.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "organization_members")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrganizationMember {

	@EmbeddedId
	@EqualsAndHashCode.Include
	private OrganizationMemberId id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "role", nullable = false, columnDefinition = "user_role")
	private UserRole role = UserRole.DEVELOPER;

	@Setter(AccessLevel.NONE)
	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	@PrePersist
	void onCreate() {
		if (joinedAt == null) {
			joinedAt = LocalDateTime.now();
		}
	}
}
