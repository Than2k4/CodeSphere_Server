package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.PostViewKey;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "post_views", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"post_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostViewEntity {

	@EmbeddedId
	private PostViewKey id = new PostViewKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("postId")
	@JoinColumn(name = "post_id")
	private PostEntity post;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private UserEntity user;

	@Column(nullable = false)
	private Instant viewedAt = Instant.now();

	public PostViewKey getId() { 
		if (id == null) {
			id = new PostViewKey();
		}
		return id; 
	}
	public void setId(PostViewKey id) { this.id = id; }
	public PostEntity getPost() { return post; }
	public void setPost(PostEntity post) { 
		this.post = post; 
		if (post != null) {
			if (id == null) {
				id = new PostViewKey();
			}
			id.setPostId(post.getId());
		}
	}
	public UserEntity getUser() { return user; }
	public void setUser(UserEntity user) { 
		this.user = user; 
		if (user != null) {
			if (id == null) {
				id = new PostViewKey();
			}
			id.setUserId(user.getId());
		}
	}
	public Instant getViewedAt() { return viewedAt; }
	public void setViewedAt(Instant viewedAt) { this.viewedAt = viewedAt; }
}

