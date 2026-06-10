package com.krish.issuetracker.domain.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "labels")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Label {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "color_hex", nullable = false, length = 7)
	private String colorHex = "#808080";
}
