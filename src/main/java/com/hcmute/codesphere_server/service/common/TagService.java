package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.TagEntity;
import com.hcmute.codesphere_server.model.enums.TagType;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.repository.common.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponse> getAllTags() {
        List<TagEntity> tags = tagRepository.findAll();
        return tags.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TagResponse> getTagsByType(TagType type) {
        List<TagEntity> tags = tagRepository.findByType(type);
        return tags.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TagResponse mapToResponse(TagEntity entity) {
        return TagResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .build();
    }
}

