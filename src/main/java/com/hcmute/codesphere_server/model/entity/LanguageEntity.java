package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "languages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 30)
	private String version;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
}


