package com.gridinsight.backend.controller;

import com.gridinsight.backend.dto.CreateUserRequest;
import com.gridinsight.backend.dto.UpdateUserRequest;
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
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminService;

    public AdminUserController(AdminUserService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest req,
            @AuthenticationPrincipal String adminIdStr) {

        Long adminId = Long.valueOf(adminIdStr);
        return ResponseEntity.ok(adminService.createUser(req, adminId));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal String adminIdStr) {

        Long adminId = Long.valueOf(adminIdStr);
        return ResponseEntity.ok(adminService.updateUser(id, req, adminId));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal String adminIdStr) {

        Long adminId = Long.valueOf(adminIdStr);
        adminService.deactivateUser(id, adminId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal String adminIdStr) {

        Long adminId = Long.valueOf(adminIdStr);
        adminService.deleteUser(id, adminId);
        return ResponseEntity.noContent().build();
    }
}