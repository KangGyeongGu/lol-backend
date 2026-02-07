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
@Table(name = "room_host_history")
public class RoomHostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "from_user_id")
    private UUID fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HostChangeReason reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    public RoomHostHistory(UUID roomId, UUID fromUserId, UUID toUserId, HostChangeReason reason) {
        this.roomId = roomId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        this.changedAt = Instant.now();
    }
}
