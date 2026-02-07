package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spell")
public class Spell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_sec", nullable = false)
    private int durationSec;

    @Column(nullable = false)
    private int price;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Spell() {}

    public Spell(String name, String description, int durationSec, int price, boolean isActive) {
        this.name = name;
        this.description = description;
        this.durationSec = durationSec;
        this.price = price;
        this.isActive = isActive;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDurationSec() { return durationSec; }
    public int getPrice() { return price; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
}
