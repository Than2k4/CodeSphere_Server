package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.TagEntity;
import com.hcmute.codesphere_server.model.enums.TagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {
    Optional<TagEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    
    Optional<TagEntity> findBySlugAndType(String slug, TagType type);
    boolean existsBySlugAndType(String slug, TagType type);
    
    List<TagEntity> findByType(TagType type);
    
    @Query("SELECT t FROM TagEntity t WHERE t.name = :name AND t.type = :type")
    Optional<TagEntity> findByNameAndType(@Param("name") String name, @Param("type") TagType type);
}

