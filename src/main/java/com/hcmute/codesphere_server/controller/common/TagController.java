package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.enums.TagType;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.service.common.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${base.url}/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<DataResponse<List<TagResponse>>> getAllTags(
            @RequestParam(required = false) String type) {
        try {
            List<TagResponse> tags;
            if (type != null && !type.isEmpty()) {
                try {
                    TagType tagType = TagType.valueOf(type.toUpperCase());
                    tags = tagService.getTagsByType(tagType);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(DataResponse.error("Type không hợp lệ. Chỉ chấp nhận: POST, PROBLEM"));
                }
            } else {
                tags = tagService.getAllTags();
            }
            return ResponseEntity.ok(DataResponse.success(tags));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

