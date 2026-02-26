// file: src/main/java/com/gridinsight/backend/dto/RefreshRequest.java
package com.gridinsight.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}