package com.example.task_management.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {

    @Test
    void testCorsConfigurationAllowsExpectedOrigins() {
        SecurityConfig securityConfig = new SecurityConfig();
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/tasks");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
        assertEquals(3600L, config.getMaxAge());

        // Check Vercel domain
        assertEquals("https://task-management-frontend-lime-rho.vercel.app",
                config.checkOrigin("https://task-management-frontend-lime-rho.vercel.app"));

        // Check wildcard Vercel preview domains
        assertEquals("https://task-management-preview-123.vercel.app",
                config.checkOrigin("https://task-management-preview-123.vercel.app"));

        // Check localhost
        assertEquals("http://localhost:3000", config.checkOrigin("http://localhost:3000"));
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
        assertEquals("http://localhost:8080", config.checkOrigin("http://localhost:8080"));

        // Disallow arbitrary domains
        assertNull(config.checkOrigin("https://evil-site.com"));
    }
}
