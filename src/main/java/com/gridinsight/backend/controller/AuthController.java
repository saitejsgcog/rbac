// file: src/main/java/com/gridinsight/backend/controller/AuthController.java
package com.gridinsight.backend.controller;

import com.gridinsight.backend.dto.AuthResponse;
import com.gridinsight.backend.dto.LoginRequest;
import com.gridinsight.backend.dto.RefreshRequest;
import com.gridinsight.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    // Keep your /register as-is

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        var res = authService.login(req, getIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        var res = authService.refresh(req, getIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "false") boolean allDevices,
            HttpServletRequest http) {

        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        // Principal set in JwtAuthFilter as String.valueOf(Long userId)
        Long userId = Long.valueOf((String) auth.getPrincipal());

        authService.logout(
                UUID.fromString(sessionId),
                userId,
                allDevices,
                getIp(http),
                http.getHeader("User-Agent")
        );
        return ResponseEntity.noContent().build();
    }

    private String getIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        return h != null ? h.split(",")[0].trim() : req.getRemoteAddr();
    }
}