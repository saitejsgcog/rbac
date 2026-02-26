package com.gridinsight.backend.controller;

import com.gridinsight.backend.dto.CreateUserRequest;
import com.gridinsight.backend.dto.UserDto;
import com.gridinsight.backend.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')") // 🔒 Only Admins can access this controller
public class AdminUserController {

    private final AdminUserService adminService;

    public AdminUserController(AdminUserService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest req,
            @AuthenticationPrincipal String adminIdStr) { // userId comes as string from JwtAuthFilter

        Long adminId = Long.valueOf(adminIdStr);
        return ResponseEntity.ok(adminService.createUser(req, adminId));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
}