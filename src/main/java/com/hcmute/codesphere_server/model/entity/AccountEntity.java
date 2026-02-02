package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private UserEntity user;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(length = 255, nullable = true)
	private String password; // Nullable vì OAuth2 không có password

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false)
	private RoleEntity role;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isBlocked = false;

	@Column(nullable = false)
	@Builder.Default
	private Integer authenWith = 0;

	@Column(length = 255, nullable = true)
	private String refreshToken;

	@Column(nullable = true)
	private Instant passwordChangeAt;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isDeleted = false;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	@Builder.Default
	private Instant updatedAt = Instant.now();

	@PreUpdate
	public void touchUpdatedAt() { this.updatedAt = Instant.now(); }

}


