package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problems", indexes = {
	@Index(name = "idx_problem_code", columnList = "code"),
	@Index(name = "idx_problem_author", columnList = "author_id"),
	@Index(name = "idx_problem_level", columnList = "level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 40)
	private String code;

	@Column(nullable = false, length = 200)
	private String title;

	@Lob
	@Column(columnDefinition = "MEDIUMTEXT")
	private String content;

	@Column(nullable = false, length = 10)
	private String level; // EASY/MEDIUM/HARD

	@Column(nullable = false)
	private Integer timeLimitMs = 2000;

	@Column(nullable = false, name = "memory_limit_mb")
	@Builder.Default
	private Integer memoryLimitMb = 256; // Default: 256MB

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private UserEntity author;

	@Column(nullable = false)
	private Boolean status = true;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isPublic = true; // true = public (hiện trong problem list), false = contest-only (ẩn khỏi problem list)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isContest = false;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@ManyToMany
	@JoinTable(name = "problem_categories",
			joinColumns = @JoinColumn(name = "problem_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<CategoryEntity> categories = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "problem_languages",
			joinColumns = @JoinColumn(name = "problem_id"),
			inverseJoinColumns = @JoinColumn(name = "language_id"))
	private Set<LanguageEntity> languages = new HashSet<>();

	@PreUpdate
	public void touchUpdatedAt() { this.updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }
	public String getLevel() { return level; }
	public void setLevel(String level) { this.level = level; }
	public Integer getTimeLimitMs() { return timeLimitMs; }
	public void setTimeLimitMs(Integer timeLimitMs) { this.timeLimitMs = timeLimitMs; }
	public Integer getMemoryLimitMb() { return memoryLimitMb; }
	public void setMemoryLimitMb(Integer memoryLimitMb) { this.memoryLimitMb = memoryLimitMb; }
	public UserEntity getAuthor() { return author; }
	public void setAuthor(UserEntity author) { this.author = author; }
	public Boolean getStatus() { return status; }
	public void setStatus(Boolean status) { this.status = status; }
	public Boolean getIsPublic() { return isPublic; }
	public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public Boolean getIsContest() { return isContest; }
    public void setIsContest(Boolean isContest) { this.isContest = isContest; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Set<CategoryEntity> getCategories() { return categories; }
	public void setCategories(Set<CategoryEntity> categories) { this.categories = categories; }
	public Set<LanguageEntity> getLanguages() { return languages; }
	public void setLanguages(Set<LanguageEntity> languages) { this.languages = languages; }
}


