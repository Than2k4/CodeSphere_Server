package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.CategoryResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.service.common.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${base.url}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<DataResponse<List<CategoryResponse>>> getAllCategories() {
        try {
            List<CategoryResponse> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(DataResponse.success(categories));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

