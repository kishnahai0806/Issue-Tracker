package com.krish.issuetracker.repository;

import java.util.UUID;

import com.krish.issuetracker.domain.entity.IssueWatcher;
import com.krish.issuetracker.domain.entity.IssueWatcherId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueWatcherRepository extends JpaRepository<IssueWatcher, IssueWatcherId> {

	boolean existsByIdIssueIdAndIdUserId(UUID issueId, UUID userId);

	void deleteByIdIssueIdAndIdUserId(UUID issueId, UUID userId);
}
