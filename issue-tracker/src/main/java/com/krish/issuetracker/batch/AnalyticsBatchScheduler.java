package com.krish.issuetracker.batch;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalyticsBatchScheduler {

	private final JobLauncher jobLauncher;
	private final Job analyticsSnapshotJob;

	public AnalyticsBatchScheduler(
			JobLauncher jobLauncher,
			@Qualifier("analyticsSnapshotJob") Job analyticsSnapshotJob) {
		this.jobLauncher = jobLauncher;
		this.analyticsSnapshotJob = analyticsSnapshotJob;
	}

	@Scheduled(cron = "${batch.analytics.cron:0 0 1 * * *}")
	@SchedulerLock(name = "analyticsSnapshotJob", lockAtMostFor = "55m", lockAtLeastFor = "5m")
	public void runAnalyticsSnapshot() {
		LocalDate runDate = LocalDate.now();
		JobParameters jobParameters = new JobParametersBuilder()
				.addLocalDate("date", runDate)
				.toJobParameters();

		try {
			log.info("Starting batch job {} for date {}", analyticsSnapshotJob.getName(), runDate);
			jobLauncher.run(analyticsSnapshotJob, jobParameters);
			log.info("Completed batch job {} for date {}", analyticsSnapshotJob.getName(), runDate);
		} catch (JobExecutionAlreadyRunningException ex) {
			log.info("Job already running, skipping");
		} catch (JobInstanceAlreadyCompleteException ex) {
			log.info("Job already completed today, skipping");
		} catch (Exception ex) {
			log.error("Failed to run batch job {} for date {}", analyticsSnapshotJob.getName(), runDate, ex);
		}
	}
}
