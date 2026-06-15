package com.krish.issuetracker.repository;

import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByIdAndIsActiveTrue(UUID id);
}
