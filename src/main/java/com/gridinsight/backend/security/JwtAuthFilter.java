// file: src/main/java/com/gridinsight/backend/security/JwtAuthFilter.java
package com.gridinsight.backend.security;

import com.gridinsight.backend.entity.UserSession;
import com.gridinsight.backend.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserSessionRepository sessionRepo;
    private final Duration idleTimeout;

    public JwtAuthFilter(JwtService jwtService, UserSessionRepository sessionRepo, Duration idleTimeout) {
        this.jwtService = jwtService;
        this.sessionRepo = sessionRepo;
        this.idleTimeout = idleTimeout;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);

                // JJWT 0.11.x: parse() returns Claims (getBody() already called inside JwtService)
                Claims claims = jwtService.parse(token);

                // subject (sub) is Long userId as string; sid is the session UUID; roles is List<String>
                Long userId = Long.valueOf(claims.getSubject());
                UUID sessionId = UUID.fromString((String) claims.get("sid"));

                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");

                // Validate session (revoked or idle timeout exceeded)
                Optional<UserSession> sessionOpt = sessionRepo.findById(sessionId);
                if (sessionOpt.isEmpty() || sessionOpt.get().isRevoked()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "SESSION_REVOKED");
                    return;
                }
                UserSession session = sessionOpt.get();

                if (Duration.between(session.getLastActivity(), LocalDateTime.now()).compareTo(idleTimeout) > 0) {
                    session.setRevoked(true); // enforce idle timeout by revoking
                    sessionRepo.save(session);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "SESSION_IDLE_TIMEOUT");
                    return;
                }

                // Sliding window: bump last activity
                session.setLastActivity(LocalDateTime.now());
                sessionRepo.save(session);

                var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();

                // Store principal as String userId (matches how we read it in controller)
                var authToken = new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ex) {
            // Any parsing/validation error → unauthorized
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        }

        filterChain.doFilter(request, response);
    }
}