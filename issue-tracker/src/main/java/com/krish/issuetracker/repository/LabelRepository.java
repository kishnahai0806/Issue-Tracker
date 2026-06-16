package com.krish.issuetracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, UUID> {

	List<Label> findAllByProjectId(UUID projectId);

	Optional<Label> findByIdAndProjectId(UUID labelId, UUID projectId);

	boolean existsByNameAndProjectId(String name, UUID projectId);
}
