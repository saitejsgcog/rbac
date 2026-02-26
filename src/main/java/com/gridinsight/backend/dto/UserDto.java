package com.gridinsight.backend.dto;

import com.gridinsight.backend.entity.UserStatus;
import java.util.Set;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        UserStatus status,
        Set<String> roles
) {}