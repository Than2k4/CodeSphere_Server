package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.enums.ContestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contests", indexes = {
    @Index(name = "idx_contest_author", columnList = "author_id"),
    @Index(name = "idx_contest_start_time", columnList = "start_time"),
    @Index(name = "idx_contest_end_time", columnList = "end_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "contest_type")
    private ContestType contestType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Chỉ dùng cho PRACTICE contest

    @Column(name = "start_time", nullable = true)
    private Instant startTime; // Nullable cho PRACTICE, required cho OFFICIAL

    @Column(name = "end_time", nullable = true)
    private Instant endTime; // Nullable cho PRACTICE, required cho OFFICIAL

    @Column(name = "registration_start_time", nullable = true)
    private Instant registrationStartTime; // Nullable, không cần nữa

    @Column(name = "registration_end_time", nullable = true)
    private Instant registrationEndTime; // Nullable, không cần nữa

    @Column(nullable = false, name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(length = 50, name = "access_code", nullable = true)
    private String accessCode; // Nullable, only for private contests

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(nullable = false, name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false, name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false; // true = ẩn khỏi danh sách public, admin vẫn thấy

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContestProblemEntity> contestProblems = new HashSet<>();

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContestRegistrationEntity> registrations = new HashSet<>();

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    // Helper method to get status (chỉ dùng cho OFFICIAL contest)
    // Đối với PRACTICE contest, status phụ thuộc vào user's registration (startedAt/endedAt)
    public String getStatus() {
        if (contestType == ContestType.PRACTICE) {
            // PRACTICE contest không có status chung, mỗi user có status riêng
            return "AVAILABLE";
        }
        
        // OFFICIAL contest
        Instant now = Instant.now();
        if (startTime == null || endTime == null) {
            return "AVAILABLE";
        }
        
        if (now.isBefore(startTime)) {
            return "UPCOMING";
        } else if (now.isBefore(endTime)) {
            return "ONGOING";
        } else {
            return "ENDED";
        }
    }
}

