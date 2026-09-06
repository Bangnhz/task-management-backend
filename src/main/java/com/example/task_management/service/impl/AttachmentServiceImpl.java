package com.example.task_management.service.impl;

import com.example.task_management.converter.AttachmentConverter;
import com.example.task_management.dto.response.AttachmentResponse;
import com.example.task_management.entity.AttachmentEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.AttachmentStatus;
import com.example.task_management.repository.AttachmentRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.AttachmentService;
import com.example.task_management.service.CloudinaryStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CloudinaryStorageService cloudinaryStorageService;
    @Autowired
    private AttachmentConverter attachmentConverter;
    @Autowired
    private AttachmentRepository attachmentRepository;

    @Override
    public List<AttachmentResponse> uploadFiles(List<MultipartFile> files) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        List<AttachmentResponse> result = new ArrayList<>();
        for(MultipartFile file : files){
            String fileUrl = cloudinaryStorageService.upload(file);
            UserEntity userEntity = userRepository.findById(userDetails.getId()).orElseThrow(()-> new RuntimeException("User not found" ));
            AttachmentEntity attachment = AttachmentEntity.builder()
                    .uploadedBy(userEntity)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .status(AttachmentStatus.TEMPORARY)
                    .comment(null)
                    .task(null)
                    .contentType(file.getContentType())
                    .build();
            AttachmentEntity saved = attachmentRepository.save(attachment);
            result.add(attachmentConverter.toAttachmentResponse(saved));
        }
        return result;
    }

    @Override
    public void delete(Long attachmentId) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        AttachmentEntity attachment = attachmentRepository.findById(attachmentId).orElseThrow(()-> new RuntimeException("Attachment not found" ));
        if(!attachment.getUploadedBy().getId().equals(userDetails.getId())){
            throw new RuntimeException("Permission denied: You cannot delete this file.");
        }
        attachmentRepository.delete(attachment);
    }

}

