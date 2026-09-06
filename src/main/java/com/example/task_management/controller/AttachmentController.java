package com.example.task_management.controller;

import com.example.task_management.dto.response.AttachmentResponse;
import com.example.task_management.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "*")
public class AttachmentController {
    @Autowired
    private AttachmentService attachmentService;

    @PostMapping()
    public ResponseEntity<List<AttachmentResponse>> upload(
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        List<AttachmentResponse> attachmentList = attachmentService.uploadFiles(files);
        return ResponseEntity.ok(attachmentList);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        attachmentService.delete(id);
        return ResponseEntity.ok().build();
    }

}

