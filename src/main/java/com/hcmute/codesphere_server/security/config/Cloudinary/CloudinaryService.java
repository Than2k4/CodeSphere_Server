package com.hcmute.codesphere_server.security.config.Cloudinary;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, Object> upload(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("File upload is empty!");
            }

            // Tạo unique public_id để tránh trùng tên
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Generate unique ID: timestamp + UUID + extension
            String uniqueId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
            String publicId = "codesphere/" + uniqueId + extension;

            // Upload với public_id unique
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("public_id", publicId);
            uploadParams.put("overwrite", false); // Không overwrite nếu trùng (sẽ không xảy ra vì unique ID)
            uploadParams.put("resource_type", "auto"); // Tự động detect loại file

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary
                    .uploader()
                    .upload(file.getBytes(), uploadParams);
            
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }
}
