package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.validator.CommentValidator;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "comments", indexes = {
	@Index(name = "idx_comment_post", columnList = "post_id"),
	@Index(name = "idx_comment_problem", columnList = "problem_id"),
	@Index(name = "idx_comment_author", columnList = "author_id"),
	@Index(name = "idx_comment_parent", columnList = "parent_comment_id")
})
@EntityListeners(CommentValidator.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private UserEntity author;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private PostEntity post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id")
	private ProblemEntity problem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private CommentEntity parent;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private Boolean isAccepted = false;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@PreUpdate
	public void touchUpdatedAt() { this.updatedAt = Instant.now(); }

}


