package com.krish.issuetracker.batch;

import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.repository.ProjectRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
public class BatchConfig {

	private static final int CHUNK_SIZE = 10;
	private static final String ANALYTICS_SNAPSHOT_DURATION_METRIC = "batch.analytics.snapshot.duration";

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final AnalyticsSnapshotProcessor analyticsSnapshotProcessor;
	private final AnalyticsSnapshotWriter analyticsSnapshotWriter;
	private final MeterRegistry meterRegistry;
	private final EntityManagerFactory entityManagerFactory;

	public BatchConfig(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			AnalyticsSnapshotProcessor analyticsSnapshotProcessor,
			AnalyticsSnapshotWriter analyticsSnapshotWriter,
			MeterRegistry meterRegistry,
			EntityManagerFactory entityManagerFactory) {
		this.jobRepository = jobRepository;
		this.transactionManager = transactionManager;
		this.analyticsSnapshotProcessor = analyticsSnapshotProcessor;
		this.analyticsSnapshotWriter = analyticsSnapshotWriter;
		this.meterRegistry = meterRegistry;
		this.entityManagerFactory = entityManagerFactory;
	}

	@Bean
	public Job analyticsSnapshotJob() {
		return new JobBuilder("analyticsSnapshotJob", jobRepository)
				.start(analyticsSnapshotStep())
				.build();
	}

	@Bean
	public Step analyticsSnapshotStep() {
		return new StepBuilder("analyticsSnapshotStep", jobRepository)
				.<Project, AnalyticsSnapshot>chunk(CHUNK_SIZE, transactionManager)
				.reader(projectReader())
				.processor(analyticsSnapshotProcessor)
				.writer(analyticsSnapshotWriter)
				.listener(stepDurationListener())
				.build();
	}

	private JpaCursorItemReader<Project> projectReader() {
		JpaCursorItemReader<Project> reader = new JpaCursorItemReader<>();
		reader.setName("activeProjectReader");
		reader.setEntityManagerFactory(entityManagerFactory);
		reader.setQueryString("SELECT p FROM Project p WHERE p.isArchived = false");
		return reader;
	}

	private StepExecutionListener stepDurationListener() {
		return new StepExecutionListener() {
			private Timer.Sample sample;

			@Override
			public void beforeStep(StepExecution stepExecution) {
				sample = Timer.start(meterRegistry);
			}

			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				if (sample != null) {
					sample.stop(Timer.builder(ANALYTICS_SNAPSHOT_DURATION_METRIC).register(meterRegistry));
				}
				log.info("Analytics snapshot step finished with status {}", stepExecution.getStatus());
				return stepExecution.getExitStatus();
			}
		};
	}
}
