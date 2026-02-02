package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.CommentLikeKey;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "comment_likes", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"comment_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeEntity {

	@EmbeddedId
	private CommentLikeKey id = new CommentLikeKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("commentId")
	@JoinColumn(name = "comment_id")
	private CommentEntity comment;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private UserEntity user;

	@Column(nullable = false)
	private Integer vote;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	public CommentLikeKey getId() { return id; }
	public void setId(CommentLikeKey id) { this.id = id; }
	public CommentEntity getComment() { return comment; }
	public void setComment(CommentEntity comment) { this.comment = comment; }
	public UserEntity getUser() { return user; }
	public void setUser(UserEntity user) { this.user = user; }
	public Integer getVote() { return vote; }
	public void setVote(Integer vote) { this.vote = vote; }
	public Instant getCreatedAt() { return createdAt; }
}


