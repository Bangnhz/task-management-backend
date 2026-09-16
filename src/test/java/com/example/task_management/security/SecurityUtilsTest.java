package com.example.task_management.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserId_Success() {
        CustomUserDetails userDetails = new CustomUserDetails(
                100L,
                "user@example.com",
                "hashedpassword",
                "John Doe",
                "http://avatar.url",
                Collections.emptyList()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserId();
        assertEquals(100L, userId);
    }

    @Test
    void testGetCurrentUserId_NoAuthentication_ThrowsException() {
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void testGetCurrentUserId_PrincipalNotCustomUserDetails_ThrowsException() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "plain_string_principal", null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void testGetCurrentUserDetails_Success() {
        CustomUserDetails userDetails = new CustomUserDetails(
                101L,
                "user@example.com",
                "hashedpassword",
                "Jane Doe",
                null,
                Collections.emptyList()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        CustomUserDetails result = SecurityUtils.getCurrentUserDetails();
        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("Jane Doe", result.getFullName());
    }

    @Test
    void testGetCurrentUserDetails_NoAuth_ThrowsException() {
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUserDetails);
    }

    @Test
    void testGetCurrentUser_Success() {
        CustomUserDetails userDetails = new CustomUserDetails(
                102L,
                "user@example.com",
                "hashedpassword",
                "Alice",
                null,
                Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        CustomUserDetails result = SecurityUtils.getCurrentUser();
        assertNotNull(result);
        assertEquals(102L, result.getId());
    }

    @Test
    void testGetCurrentUser_NoAuth_ReturnsNull() {
        CustomUserDetails result = SecurityUtils.getCurrentUser();
        assertNull(result);
    }

    @Test
    void testGenerateToken_ReturnsValidRandomToken() {
        String token1 = SecurityUtils.generateToken();
        String token2 = SecurityUtils.generateToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertFalse(token1.isBlank());
        assertFalse(token2.isBlank());
        assertNotEquals(token1, token2);
        assertTrue(token1.length() >= 32);
    }
}
