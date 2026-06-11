package com.library.entity.admin;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_username", columnList = "username")
})
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(length = 50)
    private String username;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
