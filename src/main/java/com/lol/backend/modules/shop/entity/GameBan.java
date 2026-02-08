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
        this.id = UUID.randomUUID();
        this.gameId = gameId;
        this.userId = userId;
        this.algorithmId = algorithmId;
    }

    /**
     * Redis 스냅샷 복원용 팩토리 메서드.
     * ID와 createdAt를 명시적으로 설정.
     */
    public static GameBan restore(UUID id, UUID gameId, UUID userId, UUID algorithmId, Instant createdAt) {
        GameBan gameBan = new GameBan(gameId, userId, algorithmId);
        gameBan.id = id;
        gameBan.createdAt = createdAt;
        return gameBan;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
