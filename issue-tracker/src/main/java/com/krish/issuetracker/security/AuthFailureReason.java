package com.krish.issuetracker.security;

/**
 * Single source of truth for the {@code auth.failures} metric and its {@code reason}
 * tag values. Every producer of the metric must reference these constants so the tag
 * cardinality stays bounded and a stray literal cannot silently spawn a new series.
 */
public enum AuthFailureReason {

	BAD_CREDENTIALS,
	ACCOUNT_DISABLED,
	TOKEN_EXPIRED,
	TOKEN_INVALID,
	TOKEN_REVOKED;

	public static final String METRIC_NAME = "auth.failures";
	public static final String REASON_TAG = "reason";
}
