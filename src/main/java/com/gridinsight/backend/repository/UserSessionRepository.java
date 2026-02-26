package com.gridinsight.backend.repository;

import com.gridinsight.backend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserIdAndRevokedFalse(Long userId);
}