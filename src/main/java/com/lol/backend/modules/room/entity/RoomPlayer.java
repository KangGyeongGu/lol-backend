package com.lol.backend.modules.room.entity;

import com.lol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerState state;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Setter
    @Column(name = "left_at")
    private Instant leftAt;

    @Setter
    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

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

    public void leave() {
        this.leftAt = Instant.now();
    }
}
