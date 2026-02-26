package com.gridinsight.backend.service;

import com.gridinsight.backend.dto.CreateUserRequest;
import com.gridinsight.backend.dto.UserDto;
import com.gridinsight.backend.entity.*;
import com.gridinsight.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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

    public List<UserDto> getAllUsers() {
        return userRepo.findAll().stream().map(this::mapToDto).toList();
    }

    private void logAction(Long adminId, String action, String resource) {
        AuditLog log = AuditLog.builder()
                .userId(adminId)
                .action(action)
                .resource(resource)
                .timestamp(LocalDateTime.now())
                .build();
        auditRepo.save(log);
    }

    private UserDto mapToDto(User u) {
        Set<String> roleNames = u.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getStatus(), roleNames);
    }
}