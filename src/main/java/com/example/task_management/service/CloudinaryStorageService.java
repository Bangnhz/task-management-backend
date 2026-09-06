package com.example.task_management.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CloudinaryStorageService {
    String upload(MultipartFile file);
}
