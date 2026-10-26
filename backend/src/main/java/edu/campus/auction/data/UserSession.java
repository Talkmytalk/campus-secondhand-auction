package edu.campus.auction.data;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id @Column(length = 64)
    public String tokenHash;
    @Column(nullable = false)
    public Long userId;
    @Column(nullable = false)
    public Instant expiresAt;
}
