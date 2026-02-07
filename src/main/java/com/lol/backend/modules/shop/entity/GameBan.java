package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * GAME_BAN 엔티티.
 * DATA_MODEL.md 5.8 기준.
 * storage: persistent (감사/통계 목적으로 보관)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_ban", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"game_id", "user_id"})
})
public class GameBan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "algorithm_id", nullable = false)
    private UUID algorithmId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GameBan(UUID gameId, UUID userId, UUID algorithmId) {
        this.gameId = gameId;
        this.userId = userId;
        this.algorithmId = algorithmId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
