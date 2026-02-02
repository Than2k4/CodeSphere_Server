package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.FollowKey;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "follows", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"follower_id", "followee_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowEntity {

	@EmbeddedId
	@lombok.Builder.Default
	private FollowKey id = new FollowKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("followerId")
	@JoinColumn(name = "follower_id")
	private UserEntity follower;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("followeeId")
	@JoinColumn(name = "followee_id")
	private UserEntity followee;

	@Column(nullable = false)
	@lombok.Builder.Default
	private Instant createdAt = Instant.now();

	// Helper methods to ensure ID is set correctly
	public FollowKey getId() {
		if (id == null) {
			id = new FollowKey();
		}
		return id;
	}
	
	public void setId(FollowKey id) {
		this.id = id;
	}
	
	public UserEntity getFollower() {
		return follower;
	}
	
	public void setFollower(UserEntity follower) {
		this.follower = follower;
		if (follower != null) {
			if (id == null) {
				id = new FollowKey();
			}
			id.setFollowerId(follower.getId());
		}
	}
	
	public UserEntity getFollowee() {
		return followee;
	}
	
	public void setFollowee(UserEntity followee) {
		this.followee = followee;
		if (followee != null) {
			if (id == null) {
				id = new FollowKey();
			}
			id.setFolloweeId(followee.getId());
		}
	}
}


