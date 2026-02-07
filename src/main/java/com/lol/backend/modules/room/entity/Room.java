package com.lol.backend.modules.room.entity;

import com.lol.backend.modules.game.entity.GameType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_name", nullable = false, length = 30)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 20)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.lol.backend.modules.user.entity.Language language;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Room() {}

    public Room(String roomName, GameType gameType,
                com.lol.backend.modules.user.entity.Language language,
                int maxPlayers, UUID hostUserId) {
        this.roomName = roomName;
        this.gameType = gameType;
        this.language = language;
        this.maxPlayers = maxPlayers;
        this.hostUserId = hostUserId;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getRoomName() { return roomName; }
    public GameType getGameType() { return gameType; }
    public com.lol.backend.modules.user.entity.Language getLanguage() { return language; }
    public int getMaxPlayers() { return maxPlayers; }
    public UUID getHostUserId() { return hostUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters for mutable fields
    public void setHostUserId(UUID hostUserId) {
        this.hostUserId = hostUserId;
    }
}
