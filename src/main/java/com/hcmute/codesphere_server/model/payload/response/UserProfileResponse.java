package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private Date dob;
    private String phoneNumber;
    private String gender;
    private Boolean status;
    private Instant lastOnline;
    private String role;
    private Integer authenWith; // 0 = Local, 1 = Google
    private Boolean isBlocked;
    private Instant createdAt;
    private Instant updatedAt;
    private Long completedProblems; // Số bài tập đã hoàn thành (AC)
    private Long contestsParticipated; // Số contest đã tham gia
}

