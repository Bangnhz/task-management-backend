package com.example.task_management.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileParseService {

    String extractContent(MultipartFile[] files) throws Exception;

    String extractContent(MultipartFile file) throws Exception;
}