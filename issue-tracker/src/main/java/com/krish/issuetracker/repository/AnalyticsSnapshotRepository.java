package com.krish.issuetracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {

	Optional<AnalyticsSnapshot> findTopByProjectIdOrderBySnapshotDateDesc(UUID projectId);

	List<AnalyticsSnapshot> findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
			UUID projectId,
			LocalDate from,
			LocalDate to);
}
