package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * SPELL_USAGE 엔티티.
 * DATA_MODEL.md 5.14 기준.
 * storage: persistent (스펠 사용 로그 보관)
 */
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

    protected SpellUsage() {}

    public SpellUsage(UUID gameId, UUID userId, UUID spellId) {
        this.gameId = gameId;
        this.userId = userId;
        this.spellId = spellId;
    }

    @PrePersist
    protected void onCreate() {
        this.usedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public UUID getUserId() { return userId; }
    public UUID getSpellId() { return spellId; }
    public Instant getUsedAt() { return usedAt; }
}
