package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * GAME_SPELL_PURCHASE 엔티티.
 * DATA_MODEL.md 5.11 기준.
 * storage: persistent (구매 이력 보관)
 */
@Entity
@Table(name = "game_spell_purchase")
public class GameSpellPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "spell_id", nullable = false)
    private UUID spellId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    protected GameSpellPurchase() {}

    public GameSpellPurchase(UUID gameId, UUID userId, UUID spellId, int quantity, int unitPrice, int totalPrice) {
        this.gameId = gameId;
        this.userId = userId;
        this.spellId = spellId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    @PrePersist
    protected void onCreate() {
        this.purchasedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public UUID getUserId() { return userId; }
    public UUID getSpellId() { return spellId; }
    public int getQuantity() { return quantity; }
    public int getUnitPrice() { return unitPrice; }
    public int getTotalPrice() { return totalPrice; }
    public Instant getPurchasedAt() { return purchasedAt; }
}
