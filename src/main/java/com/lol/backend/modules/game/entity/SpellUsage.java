package com.lol.backend.modules.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * SPELL_USAGE 엔티티.
 * DATA_MODEL.md 5.14 기준.
 * storage: persistent (스펠 사용 로그 보관)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "spell_usage")
public class SpellUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "spell_id", nullable = false)
    private UUID spellId;

    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    public SpellUsage(UUID gameId, UUID userId, UUID spellId) {
        this.gameId = gameId;
        this.userId = userId;
        this.spellId = spellId;
    }

    @PrePersist
    protected void onCreate() {
        this.usedAt = Instant.now();
    }
}
