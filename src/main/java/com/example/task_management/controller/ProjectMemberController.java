package com.example.task_management.controller;

import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.dto.request.ProjectMemberStatusRequest;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/projects")
public class ProjectMemberController {

    @Autowired
    private ProjectMemberService projectMemberService;

    @GetMapping("/{projectId}/members/mention-suggestions")
    public ResponseEntity<List<UserMentionResponse>> searchMentionUsers(
            @PathVariable("projectId") Long projectId,
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {

        List<UserMentionResponse> results = projectMemberService.searchMembersForMention(projectId, query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{projectId}/members/pending")
    public ResponseEntity<List<ProjectMemberResponse>> getPendingMembers(
            @PathVariable("projectId") Long projectId) {

        List<ProjectMemberResponse> pendingList =
                projectMemberService.getPendingMembers(projectId);

        return ResponseEntity.ok(pendingList);
    }
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getMembers(
            @PathVariable("projectId") Long projectId) {

        List<ProjectMemberResponse> members =
                projectMemberService.getMembers(projectId);

        return ResponseEntity.ok(members);
    }

    @PatchMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<ProjectMemberResponse> updateMemberStatus(
            @PathVariable("projectId") Long projectId,
            @PathVariable("memberId") Long memberId,
            @Valid @RequestBody ProjectMemberStatusRequest request) {

        ProjectMemberResponse response =
                projectMemberService.updateMemberStatus(
                        projectId,
                        memberId,
                        request.getStatus());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveProject(@PathVariable("projectId") Long projectId) {
        projectMemberService.leaveProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/kick")
    public ResponseEntity<ProjectMemberResponse> kickMember(
            @PathVariable("projectId") Long projectId,
            @PathVariable("memberId") Long memberId) {

        ProjectMemberResponse response = projectMemberService.kickMember(projectId, memberId);
        return ResponseEntity.ok(response);
    }
}

