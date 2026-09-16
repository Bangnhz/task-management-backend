package com.example.task_management.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.task_management.service.impl.CloudinaryStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryStorageServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryStorageServiceImpl storageService;

    @Test
    void testUpload_NullOrEmptyFile_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> storageService.upload(null));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> storageService.upload(emptyFile));
    }

    @Test
    void testUpload_Success_ReturnsSecureUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image_data".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://cloudinary.com/photo.jpg"));

        String resultUrl = storageService.upload(file);

        assertNotNull(resultUrl);
        assertEquals("https://cloudinary.com/photo.jpg", resultUrl);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    void testUpload_IOException_ThrowsRuntimeException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image_data".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Network error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> storageService.upload(file));
        assertTrue(ex.getMessage().contains("Không thể upload"));
    }
}
