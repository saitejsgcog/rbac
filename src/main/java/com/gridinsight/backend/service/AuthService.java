package com.gridinsight.backend.service;

import com.gridinsight.backend.dto.AuthResponse;
import com.gridinsight.backend.dto.LoginRequest;
import com.gridinsight.backend.dto.RefreshRequest;
import com.gridinsight.backend.entity.AuthAuditLog;
import com.gridinsight.backend.entity.AuthEventType;
import com.gridinsight.backend.entity.RefreshToken;
import com.gridinsight.backend.entity.UserSession;
import com.gridinsight.backend.repository.AuthAuditLogRepository;
import com.gridinsight.backend.repository.RefreshTokenRepository;
import com.gridinsight.backend.repository.UserRepository;
import com.gridinsight.backend.repository.UserSessionRepository;
import com.gridinsight.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final long REFRESH_TTL_MINUTES = 1440; // 24h

    private final UserRepository userRepo;
    private final UserSessionRepository sessionRepo;
    private final RefreshTokenRepository refreshRepo;
    private final AuthAuditLogRepository auditRepo;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo, UserSessionRepository sessionRepo,
                       RefreshTokenRepository refreshRepo, AuthAuditLogRepository auditRepo,
                       AuthenticationManager authManager, JwtService jwtService) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.refreshRepo = refreshRepo;
        this.auditRepo = auditRepo;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        var user = userRepo.findByEmailIgnoreCase(request.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("BAD_CREDENTIALS"));

        // Lock gate
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            audit(user.getId(), AuthEventType.ACCOUNT_LOCKED, "Locked until " + user.getLockedUntil(), ip, userAgent);
            throw new LockedException("ACCOUNT_LOCKED");
        }

        try {
            // Authenticate by email (AppUserDetailsService uses email as username)
            authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        } catch (AuthenticationException ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                audit(user.getId(), AuthEventType.ACCOUNT_LOCKED, "Exceeded attempts", ip, userAgent);
            } else {
                audit(user.getId(), AuthEventType.LOGIN_FAILED, "Attempt " + attempts, ip, userAgent);
            }
            userRepo.save(user);
            throw new BadCredentialsException("BAD_CREDENTIALS");
        }

        // Reset counters on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);

        // Create session (UUID id, Long userId)
        UUID sessionId = UUID.randomUUID();
        var session = new UserSession();
        session.setId(sessionId);
        session.setUserId(user.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setRevoked(false);
        sessionRepo.save(session);

        // Roles (empty list for now; add mapping later if needed)
        List<String> roles = List.of();

        // Access token (JwtService accepts Long userId)
        String access = jwtService.generateAccessToken(user.getId(), sessionId, roles);

        // Refresh token (opaque, rotated on refresh)
        var refreshPlain = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        var refresh = new RefreshToken();
        refresh.setId(UUID.randomUUID());
        refresh.setSessionId(sessionId);
        refresh.setUserId(user.getId());
        refresh.setTokenHash(sha256(refreshPlain));
        refresh.setIssuedAt(LocalDateTime.now());
        refresh.setExpiresAt(LocalDateTime.now().plusMinutes(REFRESH_TTL_MINUTES));
        refresh.setRevoked(false);
        refreshRepo.save(refresh);

        audit(user.getId(), AuthEventType.LOGIN_SUCCESS, "Session " + sessionId, ip, userAgent);

        return new AuthResponse(
                access,
                jwtService.getAccessTtlSeconds(),
                refreshPlain,
                java.time.temporal.ChronoUnit.MINUTES.getDuration().multipliedBy(REFRESH_TTL_MINUTES).getSeconds(),
                sessionId.toString()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request, String ip, String userAgent) {
        var hash = sha256(request.refreshToken());
        var rt = refreshRepo.findByTokenHash(hash).orElseThrow(() -> new BadCredentialsException("INVALID_REFRESH"));

        // Reuse/expired detection
        if (rt.isRevoked() || LocalDateTime.now().isAfter(rt.getExpiresAt())) {
            audit(rt.getUserId(), AuthEventType.REFRESH_REUSE_DETECTED, "Reuse/Expired", ip, userAgent);
            // revoke entire session & all remaining refresh tokens
            sessionRepo.findById(rt.getSessionId()).ifPresent(s -> { s.setRevoked(true); sessionRepo.save(s); });
            refreshRepo.findBySessionIdAndRevokedFalse(rt.getSessionId())
                    .forEach(t -> { t.setRevoked(true); refreshRepo.save(t); });
            throw new BadCredentialsException("INVALID_REFRESH");
        }

        // Rotate the refresh token
        rt.setRevoked(true);
        rt.setRotatedAt(LocalDateTime.now());
        refreshRepo.save(rt);

        var newPlain = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        var newRt = new RefreshToken();
        newRt.setId(UUID.randomUUID());
        newRt.setParentId(rt.getId());
        newRt.setSessionId(rt.getSessionId());
        newRt.setUserId(rt.getUserId());
        newRt.setTokenHash(sha256(newPlain));
        newRt.setIssuedAt(LocalDateTime.now());
        newRt.setExpiresAt(LocalDateTime.now().plusMinutes(REFRESH_TTL_MINUTES));
        newRt.setRevoked(false);
        refreshRepo.save(newRt);

        // Update session activity
        var session = sessionRepo.findById(rt.getSessionId())
                .orElseThrow(() -> new BadCredentialsException("SESSION_NOT_FOUND"));
        if (session.isRevoked()) {
            throw new BadCredentialsException("SESSION_REVOKED");
        }
        session.setLastActivity(LocalDateTime.now());
        sessionRepo.save(session);

        // Re-issue access (roles could be reloaded if you add RBAC later)
        var user = userRepo.findById(rt.getUserId()).orElseThrow();
        List<String> roles = List.of();
        String access = jwtService.generateAccessToken(user.getId(), session.getId(), roles);

        audit(user.getId(), AuthEventType.REFRESH_SUCCESS, "Rotated", ip, userAgent);

        return new AuthResponse(
                access,
                jwtService.getAccessTtlSeconds(),
                newPlain,
                java.time.temporal.ChronoUnit.MINUTES.getDuration().multipliedBy(REFRESH_TTL_MINUTES).getSeconds(),
                session.getId().toString()
        );
    }

    @Transactional
    public void logout(UUID sessionId, Long userId, boolean allDevices, String ip, String userAgent) {
        if (allDevices) {
            // Revoke all active sessions for this user
            sessionRepo.findByUserIdAndRevokedFalse(userId).forEach(s -> {
                s.setRevoked(true);
                sessionRepo.save(s);
            });
            // Revoke all refresh tokens for those sessions (defensive)
            sessionRepo.findByUserIdAndRevokedFalse(userId).forEach(s ->
                    refreshRepo.findBySessionIdAndRevokedFalse(s.getId())
                            .forEach(t -> { t.setRevoked(true); refreshRepo.save(t); })
            );
        } else {
            sessionRepo.findById(sessionId).ifPresent(s -> {
                s.setRevoked(true);
                sessionRepo.save(s);
                // Revoke refresh tokens tied to this session
                refreshRepo.findBySessionIdAndRevokedFalse(s.getId())
                        .forEach(t -> { t.setRevoked(true); refreshRepo.save(t); });
            });
        }
        audit(userId, AuthEventType.LOGOUT, allDevices ? "ALL_DEVICES" : "SINGLE_SESSION", ip, userAgent);
    }

    private void audit(Long userId, AuthEventType type, String details, String ip, String ua) {
        var log = new AuthAuditLog();
        log.setUserId(userId);
        log.setEventType(type);
        log.setDetails(details);
        log.setIp(ip);
        log.setUserAgent(ua);
        log.setCreatedAt(LocalDateTime.now());
        auditRepo.save(log);
    }

    private byte[] sha256(String input) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}