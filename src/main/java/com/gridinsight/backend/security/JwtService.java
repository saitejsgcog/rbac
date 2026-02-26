package com.gridinsight.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-seconds:600}") long accessTtlSeconds) {

        byte[] raw = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(raw);
        this.accessTtlSeconds = accessTtlSeconds;
    }

    /** Accepts Long userId, stores it as String in 'sub'; sessionId in 'sid'; roles claim. */
    public String generateAccessToken(Long userId, UUID sessionId, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .addClaims(Map.of(
                        "sid", sessionId.toString(),
                        "roles", roles
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** JJWT 0.11.x: parserBuilder() + getBody() */
    public Claims parse(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }
}