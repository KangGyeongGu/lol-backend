package com.lol.backend.modules.room.entity;

import com.lol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_player")
public class RoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerState state;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    protected RoomPlayer() {}

    public RoomPlayer(UUID roomId, UUID userId, PlayerState state) {
        this.roomId = roomId;
        this.userId = userId;
        this.state = state;
    }

    public RoomPlayer(UUID roomId, User user, PlayerState state) {
        this.roomId = roomId;
        this.userId = user.getId();
        this.user = user;
        this.state = state;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getUserId() { return userId; }
    public PlayerState getState() { return state; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public Instant getDisconnectedAt() { return disconnectedAt; }
    public User getUser() { return user; }

    // Setters
    public void setState(PlayerState state) { this.state = state; }

    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }

    public void setDisconnectedAt(Instant disconnectedAt) { this.disconnectedAt = disconnectedAt; }

    public void leave() {
        this.leftAt = Instant.now();
    }
}
