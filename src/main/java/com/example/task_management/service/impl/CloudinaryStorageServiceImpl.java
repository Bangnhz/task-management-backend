package com.example.task_management.service.impl;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.task_management.service.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageServiceImpl implements CloudinaryStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống!");
        }

        try {
            // Tự động nhận diện resource_type: "image", "video", hoặc "raw" (dành cho pdf, docx, zip...)
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "attachments", // Tên thư mục gom nhóm trên Cloudinary
                            "resource_type", "auto"    // Tự động phân loại loại file
                    )
            );

            // Lấy HTTPS URL trả về từ Cloudinary
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Upload file thành công lên Cloudinary: {}", secureUrl);

            return secureUrl;

        } catch (IOException e) {
            log.error("Lỗi khi upload file lên Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Không thể upload file đính kèm, vui lòng thử lại sau!");
        }
    }
}