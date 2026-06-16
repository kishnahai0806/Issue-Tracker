package com.krish.issuetracker.repository;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.IssueLabel;
import com.krish.issuetracker.domain.entity.IssueLabelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabelId> {

	void deleteByIdIssueIdAndIdLabelId(UUID issueId, UUID labelId);

	List<IssueLabel> findAllByIdIssueId(UUID issueId);
}
