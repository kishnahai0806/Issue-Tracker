package com.krish.issuetracker.websocket;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueUpdateEvent(
		UUID projectId,
		UUID issueId,
		String eventType,
		UUID changedBy) {

	@JsonCreator
	public IssueUpdateEvent(
			@JsonProperty("projectId") UUID projectId,
			@JsonProperty("issueId") UUID issueId,
			@JsonProperty("eventType") String eventType,
			@JsonProperty("changedBy") UUID changedBy) {
		this.projectId = projectId;
		this.issueId = issueId;
		this.eventType = eventType;
		this.changedBy = changedBy;
	}
}
