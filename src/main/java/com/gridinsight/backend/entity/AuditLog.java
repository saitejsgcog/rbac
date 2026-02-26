package com.gridinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // The Admin who performed the action

    @Column(nullable = false)
    private String action; // e.g., "CREATE_USER"

    @Column(length = 150)
    private String resource; // e.g., "User: john@example.com"

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "json")
    private String metadata; // Simple string for now, or JSON if configured
}