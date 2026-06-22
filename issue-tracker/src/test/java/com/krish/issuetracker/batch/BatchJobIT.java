package com.krish.issuetracker.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.TestcontainersConfiguration;
import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import com.krish.issuetracker.domain.entity.Issue;
import com.krish.issuetracker.domain.entity.Organization;
import com.krish.issuetracker.domain.entity.OrganizationMember;
import com.krish.issuetracker.domain.entity.OrganizationMemberId;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import com.krish.issuetracker.domain.enums.UserRole;
import com.krish.issuetracker.repository.AnalyticsSnapshotRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.repository.OrganizationRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBatchTest
class BatchJobIT {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private AnalyticsSnapshotRepository snapshotRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMemberRepository orgMemberRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private IssueRepository issueRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		jobRepositoryTestUtils.removeJobExecutions();
		snapshotRepository.deleteAll();
		issueRepository.deleteAll();
		projectRepository.deleteAll();
		orgMemberRepository.deleteAll();
		organizationRepository.deleteAll();
	}

	@Test
	void analyticsSnapshotJob_shouldCreateSnapshot_ForEachProject() throws Exception {
		TestData testData = createTestData("batch");
		Project project1 = createProject(testData.organization(), testData.user(), "BAT1", "Batch Project 1");
		Project project2 = createProject(testData.organization(), testData.user(), "BAT2", "Batch Project 2");
		issueRepository.saveAll(List.of(
				createIssue(project1, testData.user(), 1, IssueStatus.DONE, LocalDateTime.now()),
				createIssue(project1, testData.user(), 2, IssueStatus.DONE, LocalDateTime.now()),
				createIssue(project1, testData.user(), 3, IssueStatus.TODO, null),
				createIssue(project2, testData.user(), 1, IssueStatus.IN_PROGRESS, null)));

		JobExecution execution = jobLauncherTestUtils.launchJob(todayJobParameters());

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		List<AnalyticsSnapshot> snapshots = snapshotRepository.findAll();
		assertThat(snapshots).hasSize(2);

		AnalyticsSnapshot snap1 = snapshots.stream()
				.filter(snapshot -> snapshot.getProjectId().equals(project1.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(snap1.getTotalIssues()).isEqualTo(3);
		assertThat(snap1.getClosedIssues()).isEqualTo(2);
		assertThat(snap1.getOpenIssues()).isEqualTo(1);
		assertThat(snap1.getAvgResolutionHours()).isNotNull();
	}

	@Test
	void analyticsSnapshotJob_shouldBeIdempotent_WhenRunTwiceOnSameDay() throws Exception {
		TestData testData = createTestData("idempotent");
		Project project = createProject(testData.organization(), testData.user(), "IDEM1", "Idempotent Project");
		issueRepository.saveAll(List.of(
				createIssue(project, testData.user(), 1, IssueStatus.DONE, LocalDateTime.now()),
				createIssue(project, testData.user(), 2, IssueStatus.TODO, null)));
		JobParameters parameters = todayJobParameters();

		JobExecution first = jobLauncherTestUtils.launchJob(parameters);

		assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(snapshotRepository.findAll()).hasSize(1);

		try {
			JobExecution second = jobLauncherTestUtils.launchJob(parameters);
			assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		} catch (JobInstanceAlreadyCompleteException ex) {
			assertThat(ex).isNotNull();
		}

		List<AnalyticsSnapshot> snapshots = snapshotRepository.findAll();
		assertThat(snapshots).hasSize(1);
		assertThat(snapshots.getFirst().getTotalIssues()).isEqualTo(2);
	}

	private TestData createTestData(String prefix) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User user = new User();
		user.setEmail(prefix + "_" + suffix + "@test.com");
		user.setPasswordHash(passwordEncoder.encode("password"));
		user.setFullName("Batch Test User");
		user.setActive(true);
		user.setEmailVerified(true);
		User savedUser = userRepository.save(user);

		Organization organization = new Organization();
		organization.setName("Batch Test Org " + suffix);
		organization.setSlug("batch-test-" + suffix);
		organization.setPlan("FREE");
		organization.setActive(true);
		Organization savedOrganization = organizationRepository.save(organization);

		OrganizationMember member = new OrganizationMember();
		member.setId(new OrganizationMemberId(savedOrganization.getId(), savedUser.getId()));
		member.setRole(UserRole.ADMIN);
		orgMemberRepository.save(member);

		return new TestData(savedUser, savedOrganization);
	}

	private Project createProject(Organization organization, User user, String key, String name) {
		Project project = new Project();
		project.setOrganizationId(organization.getId());
		project.setName(name);
		project.setKey(key);
		project.setCreatedBy(user.getId());
		project.setArchived(false);
		return projectRepository.save(project);
	}

	private Issue createIssue(Project project, User user, int issueNumber, IssueStatus status, LocalDateTime resolvedAt) {
		Issue issue = new Issue();
		issue.setProjectId(project.getId());
		issue.setIssueNumber(issueNumber);
		issue.setTitle("Batch issue " + issueNumber);
		issue.setDescription("Batch analytics test issue");
		issue.setStatus(status);
		issue.setPriority(IssuePriority.MEDIUM);
		issue.setType(IssueType.TASK);
		issue.setReporterId(user.getId());
		issue.setResolvedAt(resolvedAt);
		return issue;
	}

	private JobParameters todayJobParameters() {
		return new JobParametersBuilder()
				.addLocalDate("date", LocalDate.now())
				.toJobParameters();
	}

	private record TestData(User user, Organization organization) {
	}
}
