package com.gridinsight.backend.repository;

import com.gridinsight.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(byte[] tokenHash);
    List<RefreshToken> findBySessionIdAndRevokedFalse(UUID sessionId);
}