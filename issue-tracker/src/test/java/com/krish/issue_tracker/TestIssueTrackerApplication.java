package com.krish.issue_tracker;

import org.springframework.boot.SpringApplication;

public class TestIssueTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(IssueTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
