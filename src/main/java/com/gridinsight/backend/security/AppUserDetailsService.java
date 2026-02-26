package com.gridinsight.backend.security;

import com.gridinsight.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public AppUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // If you add roles later, map them here. For now, empty authorities.
        List<SimpleGrantedAuthority> authorities = List.of();

        boolean locked = user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil());
        boolean disabled = user.getStatus() != null && user.getStatus().name().equals("DISABLED");

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())        // principal = email
                .password(user.getPasswordHash())     // encoded password
                .authorities(authorities)
                .accountLocked(locked)
                .disabled(disabled)
                .build();
    }
}