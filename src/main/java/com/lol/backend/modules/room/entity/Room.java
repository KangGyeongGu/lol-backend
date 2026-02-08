package com.lol.backend.modules.room.entity;

import com.lol.backend.modules.game.entity.GameType;
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
@Table(name = "room")
public class Room {

    @Id
    private UUID id;

    @Setter
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

    @Setter
    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Room(String roomName, GameType gameType,
                com.lol.backend.modules.user.entity.Language language,
                int maxPlayers, UUID hostUserId) {
        this.id = UUID.randomUUID();
        this.roomName = roomName;
        this.gameType = gameType;
        this.language = language;
        this.maxPlayers = maxPlayers;
        this.hostUserId = hostUserId;
    }

    public static Room restore(UUID id, String roomName, GameType gameType,
                                com.lol.backend.modules.user.entity.Language language,
                                int maxPlayers, UUID hostUserId,
                                Instant createdAt, Instant updatedAt) {
        Room room = new Room();
        room.id = id;
        room.roomName = roomName;
        room.gameType = gameType;
        room.language = language;
        room.maxPlayers = maxPlayers;
        room.hostUserId = hostUserId;
        room.createdAt = createdAt;
        room.updatedAt = updatedAt;
        return room;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            Instant now = Instant.now();
            this.createdAt = now;
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
