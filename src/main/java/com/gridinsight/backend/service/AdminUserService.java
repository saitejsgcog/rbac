package com.gridinsight.backend.service;

import com.gridinsight.backend.dto.CreateUserRequest;
import com.gridinsight.backend.dto.UpdateUserRequest;
import com.gridinsight.backend.dto.UserDto;
import com.gridinsight.backend.entity.*;
import com.gridinsight.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AuditLogRepository auditRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepo, RoleRepository roleRepo,
                            AuditLogRepository auditRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.auditRepo = auditRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. CREATE USER
    @Transactional
    public UserDto createUser(CreateUserRequest req, Long adminId) {
        if (userRepo.findByEmailIgnoreCase(req.email()).isPresent()) {
            throw new IllegalStateException("Email already exists");
        }

        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(UserStatus.ACTIVE);

        // Resolve Roles
        Set<Role> roles = new HashSet<>();
        if (req.roles() != null) {
            for (String roleNameStr : req.roles()) {
                RoleName rName = RoleName.valueOf(roleNameStr.toUpperCase());
                Role role = roleRepo.findByName(rName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleNameStr));
                roles.add(role);
            }
        }
        user.setRoles(roles);

        User saved = userRepo.save(user);

        // Audit Log
        logAction(adminId, "CREATE_USER", "Created user: " + saved.getEmail());

        return mapToDto(saved);
    }

    // 2. GET ALL USERS
    public List<UserDto> getAllUsers() {
        return userRepo.findAll().stream().map(this::mapToDto).toList();
    }

    // 3. UPDATE USER
    @Transactional
    public UserDto updateUser(Long targetUserId, UpdateUserRequest req, Long adminId) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (req.name() != null) user.setName(req.name());
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.status() != null) user.setStatus(req.status());

        // Update Roles if provided
        if (req.roles() != null && !req.roles().isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            for (String roleNameStr : req.roles()) {
                RoleName rName = RoleName.valueOf(roleNameStr.toUpperCase());
                Role role = roleRepo.findByName(rName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleNameStr));
                newRoles.add(role);
            }
            user.setRoles(newRoles);
        }

        User updatedUser = userRepo.save(user);
        logAction(adminId, "UPDATE_USER", "Updated user ID: " + targetUserId);
        return mapToDto(updatedUser);
    }

    // 4. DEACTIVATE USER
    @Transactional
    public void deactivateUser(Long targetUserId, Long adminId) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setStatus(UserStatus.INACTIVE);
        userRepo.save(user);

        logAction(adminId, "DEACTIVATE_USER", "Deactivated user ID: " + targetUserId);
    }

    // 5. DELETE USER
    @Transactional
    public void deleteUser(Long targetUserId, Long adminId) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        userRepo.delete(user);

        logAction(adminId, "DELETE_USER", "Deleted user ID: " + targetUserId);
    }

    // HELPER: AUDIT LOGGING
    private void logAction(Long adminId, String action, String resource) {
        AuditLog log = AuditLog.builder()
                .userId(adminId)
                .action(action)
                .resource(resource)
                .timestamp(LocalDateTime.now())
                .build();
        auditRepo.save(log);
    }

    // HELPER: MAP ENTITY TO DTO
    private UserDto mapToDto(User u) {
        Set<String> roleNames = u.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getStatus(), roleNames);
    }
}