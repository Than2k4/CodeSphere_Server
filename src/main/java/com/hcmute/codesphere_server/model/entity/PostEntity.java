package com.hcmute.codesphere_server.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts", indexes = {
	@Index(name = "idx_post_author", columnList = "author_id"),
	@Index(name = "idx_post_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PostEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private UserEntity author;

	@Column(nullable = false, length = 200)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(length = 500)
	private String imageUrl; // URL hình ảnh đính kèm (deprecated, dùng images)

	@Lob
	@Column(columnDefinition = "TEXT")
	private String images; // JSON array of image URLs: ["url1", "url2", ...]

	@Column(length = 500)
	private String fileUrl; // URL file đính kèm

	@Column(length = 100)
	private String fileName; // Tên file gốc

	@Column(length = 50)
	private String fileType; // Loại file (pdf, doc, etc.)

	@Column(nullable = false)
	private Boolean isAnonymous = false;

	@Column(nullable = false)
	private Boolean isBlocked = false;

	@Column(nullable = false)
	private Boolean isResolved = false;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@ManyToMany
	@JoinTable(name = "post_tags",
			joinColumns = @JoinColumn(name = "post_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<TagEntity> tags = new HashSet<>();

	@PreUpdate
	public void touchUpdatedAt() { this.updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public UserEntity getAuthor() { return author; }
	public void setAuthor(UserEntity author) { this.author = author; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }
	public String getImageUrl() { return imageUrl; }
	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
	public String getImages() { return images; }
	public void setImages(String images) { this.images = images; }
	public Boolean getAnonymous() { return isAnonymous; }
	public void setAnonymous(Boolean anonymous) { isAnonymous = anonymous; }
	public Boolean getBlocked() { return isBlocked; }
	public void setBlocked(Boolean blocked) { isBlocked = blocked; }
	public Boolean getResolved() { return isResolved; }
	public void setResolved(Boolean resolved) { isResolved = resolved; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Set<TagEntity> getTags() { return tags; }
	public void setTags(Set<TagEntity> tags) { this.tags = tags; }
}


