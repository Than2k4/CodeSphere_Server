package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 50)
	private String username;

	@Column(nullable = false, length = 255)
	private String avatar;

	@Column(nullable = true)
	private java.sql.Date dob;

	@Column(unique = true, length = 20, nullable = true)
	private String phoneNumber; // Nullable vì OAuth2 user có thể không có phone

	@Column(nullable = true)
	private String gender; // Có thể dùng ENUM nếu muốn

	@Column(nullable = false)
	@Builder.Default
	private Boolean status = true;

	@Column(nullable = false)
	@Builder.Default
	private Instant lastOnline = Instant.now();

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


