package com.example.task_management.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    // Valid 256-bit secret key in Base64
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
    }

    @Test
    void testGenerateTokenFromEmail_AndValidate() {
        String email = "test@example.com";
        Long userId = 123L;

        String token = jwtUtils.generateTokenFromEmail(email, userId);
        assertNotNull(token);
        assertFalse(token.isBlank());

        boolean isValid = jwtUtils.validateJwtToken(token);
        assertTrue(isValid);

        String extractedEmail = jwtUtils.getEmailFromJwtToken(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testGenerateJwtToken_FromAuthentication_AndValidate() {
        CustomUserDetails userDetails = new CustomUserDetails(
                55L,
                "auth@example.com",
                "password",
                "Auth User",
                null,
                Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtUtils.generateJwtToken(authentication);
        assertNotNull(token);

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("auth@example.com", jwtUtils.getEmailFromJwtToken(token));
    }

    @Test
    void testValidateJwtToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtils.validateJwtToken("invalid.token.structure"));
        assertFalse(jwtUtils.validateJwtToken(""));
        assertFalse(jwtUtils.validateJwtToken(null));
    }

    @Test
    void testValidateJwtToken_ExpiredToken_ReturnsFalse() {
        // Create an instance with negative expiration
        JwtUtils expiredJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(expiredJwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(expiredJwtUtils, "jwtExpirationMs", -1000L);

        String expiredToken = expiredJwtUtils.generateTokenFromEmail("expired@example.com", 1L);
        assertFalse(jwtUtils.validateJwtToken(expiredToken));
    }

    @Test
    void testValidateJwtToken_TamperedToken_ReturnsFalse() {
        String token = jwtUtils.generateTokenFromEmail("tamper@example.com", 1L);
        String tamperedToken = token + "xyz";
        assertFalse(jwtUtils.validateJwtToken(tamperedToken));
    }
}
