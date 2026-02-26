package com.gridinsight.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        String phone,
        @NotBlank @Size(min = 8) String password,
        Set<String> roles // e.g. ["ADMIN", "GRID_ANALYST"]
) {}