package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.CategoryEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateCategoryRequest;
import com.hcmute.codesphere_server.model.payload.response.CategoryResponse;
import com.hcmute.codesphere_server.repository.common.CategoryRepository;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Tự động tạo slug từ name
        String slug = SlugUtils.generateSlug(request.getName());
        
        // Nếu slug trống hoặc đã tồn tại, thêm số vào cuối
        String finalSlug = slug;
        int counter = 1;
        while (finalSlug.isEmpty() || categoryRepository.existsBySlug(finalSlug)) {
            finalSlug = slug + "-" + counter;
            counter++;
        }

        // Tạo category mới (không có parent)
        CategoryEntity category = CategoryEntity.builder()
                .name(request.getName())
                .slug(finalSlug)
                .build();

        category = categoryRepository.save(category);

        // Map sang response
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(null)
                .parentName(null)
                .build();
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại"));

        // Kiểm tra category có đang được dùng bởi problems không
        List<com.hcmute.codesphere_server.model.entity.ProblemEntity> problems = problemRepository.findAll().stream()
                .filter(p -> p.getCategories() != null && 
                        p.getCategories().stream().anyMatch(c -> c.getId().equals(categoryId)))
                .collect(Collectors.toList());

        if (!problems.isEmpty()) {
            List<String> problemTitles = problems.stream()
                    .map(p -> p.getTitle())
                    .limit(5)
                    .collect(Collectors.toList());
            
            String message = "Cannot delete category. It is currently used by " + problems.size() + 
                    " problem(s)";
            if (!problemTitles.isEmpty()) {
                message += ": " + String.join(", ", problemTitles);
                if (problems.size() > 5) {
                    message += " and " + (problems.size() - 5) + " more";
                }
            }
            message += ". Please remove this category from problems first.";
            
            throw new RuntimeException(message);
        }

        // Xóa category (không cần check sub-categories vì không còn parent-child relationship)
        categoryRepository.delete(category);
    }
}

