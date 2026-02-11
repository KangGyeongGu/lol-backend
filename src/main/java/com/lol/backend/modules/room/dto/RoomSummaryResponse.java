package com.lol.backend.modules.room.dto;

import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomStatus;
import com.lol.backend.modules.user.entity.Language;

import java.time.Instant;
import java.util.UUID;

public record RoomSummaryResponse(
        String roomId,
        String roomName,
        GameType gameType,
        Language language,
        int maxPlayers,
        int currentPlayers,
        RoomStatus roomStatus,
        boolean joinable,
        Instant updatedAt
) {
    public static RoomSummaryResponse from(Room room, int currentPlayers,
                                            boolean hasActiveGame,
                                            boolean isKicked) {
        RoomStatus status = hasActiveGame ? RoomStatus.IN_GAME : RoomStatus.WAITING;
        boolean joinable = !hasActiveGame
                && !isKicked
                && currentPlayers < room.getMaxPlayers();

        return new RoomSummaryResponse(
                room.getId().toString(),
                room.getRoomName(),
                room.getGameType(),
                room.getLanguage(),
                room.getMaxPlayers(),
                currentPlayers,
                status,
                joinable,
                room.getUpdatedAt()
        );
    }
}
