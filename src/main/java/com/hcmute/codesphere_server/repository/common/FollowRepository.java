package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.FollowEntity;
import com.hcmute.codesphere_server.model.entity.embedded.FollowKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, FollowKey> {
    
    @Query("SELECT f FROM FollowEntity f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId")
    Optional<FollowEntity> findByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
    
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FollowEntity f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId")
    boolean existsByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
    
    @Query("SELECT f FROM FollowEntity f WHERE f.follower.id = :followerId")
    List<FollowEntity> findByFollowerId(@Param("followerId") Long followerId);
    
    @Query("SELECT f FROM FollowEntity f WHERE f.followee.id = :followeeId")
    List<FollowEntity> findByFolloweeId(@Param("followeeId") Long followeeId);
    
    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.follower.id = :followerId")
    Long countByFollowerId(@Param("followerId") Long followerId);
    
    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.followee.id = :followeeId")
    Long countByFolloweeId(@Param("followeeId") Long followeeId);
}

