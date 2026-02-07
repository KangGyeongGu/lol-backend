package com.lol.backend.modules.room.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

    protected RoomHostHistory() {}

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

    // Getters
    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getFromUserId() { return fromUserId; }
    public UUID getToUserId() { return toUserId; }
    public HostChangeReason getReason() { return reason; }
    public Instant getChangedAt() { return changedAt; }
}
