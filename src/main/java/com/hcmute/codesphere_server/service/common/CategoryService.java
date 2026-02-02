package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.CategoryEntity;
import com.hcmute.codesphere_server.model.payload.response.CategoryResponse;
import com.hcmute.codesphere_server.repository.common.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {
        List<CategoryEntity> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToResponse(CategoryEntity entity) {
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .parentId(null) // Không còn parent nữa
                .parentName(null)
                .children(null) // Không còn children nữa
                .build();
    }
}

