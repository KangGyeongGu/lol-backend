package com.lol.backend.modules.room.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public RoomKick(UUID roomId, UUID userId, UUID kickedByUserId) {
        this.roomId = roomId;
        this.userId = userId;
        this.kickedByUserId = kickedByUserId;
    }

    @PrePersist
    protected void onCreate() {
        this.kickedAt = Instant.now();
    }
}
