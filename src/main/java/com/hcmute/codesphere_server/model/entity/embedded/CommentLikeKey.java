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
public class CommentLikeKey implements Serializable {
	@Column(name = "comment_id")
	private Long commentId;
	@Column(name = "user_id")
	private Long userId;
	
	public Long getCommentId() { return commentId; }
	public void setCommentId(Long commentId) { this.commentId = commentId; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CommentLikeKey that = (CommentLikeKey) o;
		return java.util.Objects.equals(commentId, that.commentId) &&
				java.util.Objects.equals(userId, that.userId);
	}
	
	@Override
	public int hashCode() { 
		return java.util.Objects.hash(commentId, userId); 
	}
}

