package com.hcmute.codesphere_server.model.validator;

import com.hcmute.codesphere_server.model.entity.CommentEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Custom validation để đảm bảo Comment phải liên kết với Post hoặc Problem
 * Một comment không thể tồn tại mà không có post_id hoặc problem_id
 */
public class CommentValidator {
    
    @PrePersist
    @PreUpdate
    public void validate(CommentEntity comment) {
        // Kiểm tra rằng comment phải liên kết với post hoặc problem
        if (comment.getPost() == null && comment.getProblem() == null) {
            throw new IllegalArgumentException("Comment must be associated with either a Post or a Problem");
        }
    }
}

