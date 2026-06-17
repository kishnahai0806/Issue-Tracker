package com.krish.issuetracker.config;

import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

	// === Metric Tag Strategy ===
	// auth.failures         → tag: reason
	//   values: BAD_CREDENTIALS, ACCOUNT_DISABLED,
	//           TOKEN_EXPIRED, TOKEN_INVALID
	// file.upload.validation.failure → tag: reason (already wired)
	// issues.created        → no tags (low cardinality, context in MDC)
	// issues.closed         → no tags
	// comments.added        → no tags
	// emails.sent           → no tags
	// ws.connections.active → no tags (pod-level gauge,
	//   sum across pods in Grafana: sum(ws_connections_active))
	// ==========================================
	// Metrics already wired inline — do NOT register here:
	//   storage.upload.duration      (AttachmentService)
	//   file.upload.validation.failure (GlobalExceptionHandler)
	//   batch.analytics.snapshot.duration (BatchConfig)

	@Bean
	public AtomicInteger wsActiveConnections() {
		return new AtomicInteger(0);
	}

	@Bean
	public MeterBinder applicationMetrics(AtomicInteger wsActiveConnections) {
		return registry -> {
			Counter.builder("issues.created")
					.description("Total number of issues created")
					.register(registry);
			Counter.builder("issues.closed")
					.description("Total number of issues closed")
					.register(registry);
			Counter.builder("comments.added")
					.description("Total number of comments added")
					.register(registry);
			Counter.builder("auth.failures")
					.description("Total authentication failures - tag reason set at increment site")
					.register(registry);
			Counter.builder("emails.sent")
					.description("Total emails sent successfully")
					.register(registry);
			Gauge.builder("ws.connections.active", wsActiveConnections, AtomicInteger::get)
					.description("Active WebSocket connections on this replica - sum across pods in Grafana")
					.register(registry);
		};
	}
}
