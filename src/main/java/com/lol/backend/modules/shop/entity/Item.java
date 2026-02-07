package com.lol.backend.modules.shop.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "item")
public class Item {

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

    public Item(String name, String description, int durationSec, int price, boolean isActive) {
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
}
