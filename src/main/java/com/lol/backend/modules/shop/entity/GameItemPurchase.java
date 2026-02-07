package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * GAME_ITEM_PURCHASE 엔티티.
 * DATA_MODEL.md 5.10 기준.
 * storage: persistent (구매 이력 보관)
 */
@Entity
@Table(name = "game_item_purchase")
public class GameItemPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    protected GameItemPurchase() {}

    public GameItemPurchase(UUID gameId, UUID userId, UUID itemId, int quantity, int unitPrice, int totalPrice) {
        this.gameId = gameId;
        this.userId = userId;
        this.itemId = itemId;
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
    public UUID getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public int getUnitPrice() { return unitPrice; }
    public int getTotalPrice() { return totalPrice; }
    public Instant getPurchasedAt() { return purchasedAt; }
}
