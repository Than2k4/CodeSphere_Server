package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {
    
    @NotNull(message = "Vote không được để trống")
    private Integer vote; // 1 = upvote, -1 = downvote, 0 = remove vote
}

