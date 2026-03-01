package com.gridinsight.backend.dto;

import com.gridinsight.backend.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateUserRequest(
        String name,
        String phone,
        @NotNull UserStatus status,
        Set<String> roles
) {}