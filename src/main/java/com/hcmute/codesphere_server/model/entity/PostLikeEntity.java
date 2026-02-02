package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.PostLikeKey;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "post_likes", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"post_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeEntity {

	@EmbeddedId
	private PostLikeKey id = new PostLikeKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("postId")
	@JoinColumn(name = "post_id")
	private PostEntity post;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private UserEntity user;

	@Column(nullable = false)
	private Integer vote; // +1/-1

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	public PostLikeKey getId() { 
		if (id == null) {
			id = new PostLikeKey();
		}
		return id; 
	}
	public void setId(PostLikeKey id) { this.id = id; }
	public PostEntity getPost() { return post; }
	public void setPost(PostEntity post) { 
		this.post = post; 
		if (post != null) {
			if (id == null) {
				id = new PostLikeKey();
			}
			id.setPostId(post.getId());
		}
	}
	public UserEntity getUser() { return user; }
	public void setUser(UserEntity user) { 
		this.user = user; 
		if (user != null) {
			if (id == null) {
				id = new PostLikeKey();
			}
			id.setUserId(user.getId());
		}
	}
	public Integer getVote() { return vote; }
	public void setVote(Integer vote) { this.vote = vote; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}


