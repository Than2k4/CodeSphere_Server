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
public class FollowKey implements Serializable {
	@Column(name = "follower_id")
	private Long followerId;
	@Column(name = "followee_id")
	private Long followeeId;
	
	public Long getFollowerId() { return followerId; }
	public void setFollowerId(Long followerId) { this.followerId = followerId; }
	public Long getFolloweeId() { return followeeId; }
	public void setFolloweeId(Long followeeId) { this.followeeId = followeeId; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FollowKey that = (FollowKey) o;
		return java.util.Objects.equals(followerId, that.followerId) &&
				java.util.Objects.equals(followeeId, that.followeeId);
	}
	
	@Override
	public int hashCode() {
		return java.util.Objects.hash(followerId, followeeId);
	}
}

