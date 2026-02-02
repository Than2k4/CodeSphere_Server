package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserEntity user;

	@Column(nullable = false)
	private Integer solvedTotal = 0;

	@Column(nullable = false)
	private Integer solvedEasy = 0;

	@Column(nullable = false)
	private Integer solvedMedium = 0;

	@Column(nullable = false)
	private Integer solvedHard = 0;

	@Column
	private java.time.Instant lastSolvedAt;

	@Column(nullable = false)
	private Boolean isDeleted = false;
}


