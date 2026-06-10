package com.krish.issuetracker.domain.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IssueWatcherId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "issue_id", nullable = false)
	private UUID issueId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;
}
