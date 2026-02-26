package com.gridinsight.backend.security;

import com.gridinsight.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import this

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public AppUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    @Transactional // Essential for loading the @ManyToMany roles lazily/eagerly within session
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // --- UPDATED CODE: Map DB Roles to Spring Security Authorities ---
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());
        // ----------------------------------------------------------------

        boolean locked = user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil());
        boolean disabled = user.getStatus() != null && user.getStatus().name().equals("INACTIVE"); // Map INACTIVE to disabled

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(locked)
                .disabled(disabled)
                .build();
    }
}