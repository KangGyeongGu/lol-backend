package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * GAME_PICK 엔티티.
 * DATA_MODEL.md 5.9 기준.
 * storage: persistent (감사/통계 목적으로 보관)
 */
@Entity
@Table(name = "game_pick", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"game_id", "user_id"})
})
public class GamePick {

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

    protected GamePick() {}

    public GamePick(UUID gameId, UUID userId, UUID algorithmId) {
        this.gameId = gameId;
        this.userId = userId;
        this.algorithmId = algorithmId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public UUID getUserId() { return userId; }
    public UUID getAlgorithmId() { return algorithmId; }
    public Instant getCreatedAt() { return createdAt; }
}
