// file: src/main/java/com/gridinsight/backend/dto/AuthResponse.java
package com.gridinsight.backend.dto;

public record AuthResponse(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds,
        String sessionId
) {}