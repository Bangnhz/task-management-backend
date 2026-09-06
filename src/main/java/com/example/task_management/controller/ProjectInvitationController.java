package com.example.task_management.controller;

import com.example.task_management.dto.request.ProjectInvitationCreateRequest;
import com.example.task_management.dto.response.InvitePreviewResponse;
import com.example.task_management.dto.response.ProjectInvitationResponse;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.service.ProjectInvitationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project-invitations")
@CrossOrigin(origins = "*")
public class ProjectInvitationController {
    @Autowired
    private ProjectInvitationService projectInvitationService;

    @PostMapping("/{projectId}")
    public ResponseEntity<ProjectInvitationResponse> createInvitation(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody(required = false) ProjectInvitationCreateRequest request) {

        ProjectInvitationResponse response = projectInvitationService.createInvitation(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{token}/preview")
    public ResponseEntity<InvitePreviewResponse> getPreview(@PathVariable("token") String token) {
        InvitePreviewResponse response = projectInvitationService.getInvitePreview(token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectInvitationResponse> getActiveInvitationByUser(
            @PathVariable("projectId") Long projectId) {

        ProjectInvitationResponse response = projectInvitationService.getActiveInvitationByUser(projectId);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/{token}/accept")
    public ResponseEntity<ProjectMemberResponse> acceptInvitation(
            @PathVariable("token") String token) {
        ProjectMemberResponse response = projectInvitationService.acceptInvitation(token);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/{token}/cancel")
    public ResponseEntity<ProjectMemberResponse> cancelJoinRequest(
            @PathVariable("token") String token) {
        projectInvitationService.cancelJoinRequest(token);
        return ResponseEntity.noContent().build();
    }
}

