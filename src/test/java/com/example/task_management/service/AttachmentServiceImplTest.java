package com.example.task_management.service;

import com.example.task_management.converter.AttachmentConverter;
import com.example.task_management.dto.response.AttachmentResponse;
import com.example.task_management.entity.AttachmentEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.AttachmentStatus;
import com.example.task_management.repository.AttachmentRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.AttachmentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryStorageService cloudinaryStorageService;

    @Mock
    private AttachmentConverter attachmentConverter;

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private final Long userId = 1L;
    private UserEntity user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(userId).fullName("User").email("u@example.com").build();
        userDetails = new CustomUserDetails(userId, "u@example.com", "pass", "User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUploadFiles_Success() {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.png", "image/png", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "file2.pdf", "application/pdf", "content2".getBytes());

        when(cloudinaryStorageService.upload(file1)).thenReturn("http://cloud.com/file1.png");
        when(cloudinaryStorageService.upload(file2)).thenReturn("http://cloud.com/file2.pdf");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AttachmentEntity saved1 = AttachmentEntity.builder().id(10L).fileName("file1.png").status(AttachmentStatus.TEMPORARY).build();
        AttachmentEntity saved2 = AttachmentEntity.builder().id(20L).fileName("file2.pdf").status(AttachmentStatus.TEMPORARY).build();

        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(saved1, saved2);
        when(attachmentConverter.toAttachmentResponse(saved1)).thenReturn(AttachmentResponse.builder().id(10L).fileName("file1.png").build());
        when(attachmentConverter.toAttachmentResponse(saved2)).thenReturn(AttachmentResponse.builder().id(20L).fileName("file2.pdf").build());

        List<AttachmentResponse> responses = attachmentService.uploadFiles(List.of(file1, file2));

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(10L, responses.get(0).getId());
        assertEquals(20L, responses.get(1).getId());
        verify(attachmentRepository, times(2)).save(any(AttachmentEntity.class));
    }

    @Test
    void testDelete_Success() {
        AttachmentEntity attachment = AttachmentEntity.builder()
                .id(10L)
                .uploadedBy(user)
                .build();

        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

        attachmentService.delete(10L);

        verify(attachmentRepository, times(1)).delete(attachment);
    }

    @Test
    void testDelete_PermissionDenied() {
        UserEntity otherUser = UserEntity.builder().id(99L).build();
        AttachmentEntity attachment = AttachmentEntity.builder()
                .id(10L)
                .uploadedBy(otherUser)
                .build();

        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> attachmentService.delete(10L));
        assertTrue(ex.getMessage().contains("Permission denied"));
        verify(attachmentRepository, never()).delete(any());
    }

    @Test
    void testDelete_NotFound_ThrowsException() {
        when(attachmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> attachmentService.delete(10L));
    }
}
