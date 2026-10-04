package com.kasibridge.auth.entity;

import com.kasibridge.auth.security.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "app_users",
        indexes = {
                @Index(name = "idx_auth_username", columnList = "username"),
                @Index(name = "idx_auth_org_id", columnList = "org_id")
        }
)
@Getter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "org_id", nullable = false, length = 100)
    private String orgId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "app_user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 60)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static AppUser create(
            String username,
            String passwordHash,
            String orgId,
            Set<Role> roles
    ) {
        AppUser user = new AppUser();
        user.username = username;
        user.passwordHash = passwordHash;
        user.orgId = orgId;
        user.enabled = true;
        user.roles = roles != null ? roles : new HashSet<>();
        return user;
    }
}