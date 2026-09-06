package com.example.task_management.service;

import com.example.task_management.dto.response.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AttachmentService {

    List<AttachmentResponse> uploadFiles(List<MultipartFile> files);
    void delete(Long attachmentId);
}