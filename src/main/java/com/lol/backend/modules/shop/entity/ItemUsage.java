package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * ITEM_USAGE 엔티티.
 * DATA_MODEL.md 5.13 기준.
 * storage: persistent (아이템 사용 로그 보관)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "item_usage")
public class ItemUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    public ItemUsage(UUID gameId, UUID fromUserId, UUID toUserId, UUID itemId) {
        this.gameId = gameId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.itemId = itemId;
    }

    @PrePersist
    protected void onCreate() {
        this.usedAt = Instant.now();
    }
}
