package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.enums.TagType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tags", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"slug", "type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false, length = 120)
	private String slug;

	@Column(nullable = true, length = 20) // Tạm thời nullable để migration
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private TagType type = TagType.PROBLEM; // Default là PROBLEM (tags cũ và tags từ admin)

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getSlug() { return slug; }
	public void setSlug(String slug) { this.slug = slug; }
	public TagType getType() { return type; }
	public void setType(TagType type) { this.type = type; }
}


