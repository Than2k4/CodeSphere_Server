package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.security.config.Cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${base.url}/files")
@RequiredArgsConstructor
public class FileController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload file (hỗ trợ cả image và file)
     * POST /api/v1/files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<DataResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(DataResponse.error("File không được để trống"));
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.upload(file);
            String fileUrl = (String) uploadResult.get("url");
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", fileName != null ? fileName : "file");
            response.put("fileType", fileType != null ? fileType : "application/octet-stream");
            response.put("fileSize", String.valueOf(file.getSize()));

            return ResponseEntity.ok(DataResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi khi upload file: " + e.getMessage()));
        }
    }

    /**
     * Upload image only (tối ưu cho hình ảnh)
     * POST /api/v1/files/upload-image
     */
    @PostMapping("/upload-image")
    public ResponseEntity<DataResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(DataResponse.error("File không được để trống"));
            }

            // Kiểm tra file có phải là image không
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(DataResponse.error("File phải là hình ảnh"));
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.upload(file);
            String fileUrl = (String) uploadResult.get("url");
            String fileName = file.getOriginalFilename();

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", fileName != null ? fileName : "image");
            response.put("fileType", contentType);
            response.put("fileSize", String.valueOf(file.getSize()));

            return ResponseEntity.ok(DataResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi khi upload hình ảnh: " + e.getMessage()));
        }
    }
}

