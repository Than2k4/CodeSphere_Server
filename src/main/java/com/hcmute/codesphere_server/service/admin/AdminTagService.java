package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.TagEntity;
import com.hcmute.codesphere_server.model.enums.TagType;
import com.hcmute.codesphere_server.model.payload.request.CreateTagRequest;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.repository.common.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagRepository tagRepository;

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        // Admin tạo tag cho problems, nên type mặc định là PROBLEM
        TagType tagType = TagType.PROBLEM;
        
        // Kiểm tra slug đã tồn tại với type này chưa
        if (tagRepository.existsBySlugAndType(request.getSlug().toLowerCase(), tagType)) {
            throw new RuntimeException("Tag với slug '" + request.getSlug() + "' đã tồn tại cho loại " + tagType);
        }

        // Tạo tag mới
        TagEntity tag = TagEntity.builder()
                .name(request.getName())
                .slug(request.getSlug().toLowerCase()) // Chuyển về lowercase
                .type(tagType) // Tag cho problems
                .build();

        tag = tagRepository.save(tag);

        // Map sang response
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }
}

