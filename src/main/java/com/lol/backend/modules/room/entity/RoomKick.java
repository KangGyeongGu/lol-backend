package com.lol.backend.modules.room.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_kick")
public class RoomKick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "kicked_by_user_id", nullable = false)
    private UUID kickedByUserId;

    @Column(name = "kicked_at", nullable = false, updatable = false)
    private Instant kickedAt;

    protected RoomKick() {}

    public RoomKick(UUID roomId, UUID userId, UUID kickedByUserId) {
        this.roomId = roomId;
        this.userId = userId;
        this.kickedByUserId = kickedByUserId;
    }

    @PrePersist
    protected void onCreate() {
        this.kickedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getUserId() { return userId; }
    public UUID getKickedByUserId() { return kickedByUserId; }
    public Instant getKickedAt() { return kickedAt; }
}
