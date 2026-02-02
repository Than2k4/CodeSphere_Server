package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPublicProfileResponse {
    private Long userId;
    private String username;
    private String avatar;
    private Date dob;
    private String gender;
    private Instant lastOnline;
    private Long postCount;
    private Boolean isFollowing; // null nếu chưa đăng nhập hoặc chính mình
    private Long followerCount;
    private Long followingCount;
    private Instant createdAt;
}

