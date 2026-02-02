package com.hcmute.codesphere_server.model.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeKey implements Serializable {
	@Column(name = "post_id")
	private Long postId;
	@Column(name = "user_id")
	private Long userId;
	
	public Long getPostId() { return postId; }
	public void setPostId(Long postId) { this.postId = postId; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PostLikeKey that = (PostLikeKey) o;
		return java.util.Objects.equals(postId, that.postId) &&
				java.util.Objects.equals(userId, that.userId);
	}
	
	@Override
	public int hashCode() { 
		return java.util.Objects.hash(postId, userId); 
	}
}

