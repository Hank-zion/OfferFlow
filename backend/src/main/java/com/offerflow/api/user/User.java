package com.offerflow.api.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String username;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    protected User() { }
    public User(String username, String email, String passwordHash) { this.username = username; this.email = email; this.passwordHash = passwordHash; }
    @PrePersist void created() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public String getUsername() { return username; }
    public String getEmail() { return email; } public String getPasswordHash() { return passwordHash; }
}
