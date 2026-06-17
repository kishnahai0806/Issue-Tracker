package com.krish.issuetracker.issue;

import java.util.UUID;

import com.krish.issuetracker.exception.IssueNumberGenerationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class IssueNumberGenerator {

	private final EntityManager entityManager;

	public IssueNumberGenerator(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Transactional
	public int generateNextIssueNumber(UUID projectId) {
		try {
			Query query = entityManager.createNativeQuery(
					"UPDATE projects " +
							"SET next_issue_number = next_issue_number + 1 " +
							"WHERE id = :projectId " +
							"RETURNING next_issue_number");
			query.setParameter("projectId", projectId);
			Object result = query.getSingleResult();

			if (result == null) {
				throw new IssueNumberGenerationException(projectId);
			}

			return ((Number) result).intValue();
		} catch (PersistenceException | ClassCastException ex) {
			log.debug("Issue number generation failed for project {}", projectId, ex);
			throw new IssueNumberGenerationException(projectId);
		}
	}
}
