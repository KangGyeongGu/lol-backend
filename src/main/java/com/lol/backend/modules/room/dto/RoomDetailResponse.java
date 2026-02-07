package com.lol.backend.modules.room.dto;

import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.user.entity.Language;

import java.util.List;

public record RoomDetailResponse(
        String roomId,
        String roomName,
        GameType gameType,
        Language language,
        int maxPlayers,
        List<RoomPlayerResponse> players
) {
    public static RoomDetailResponse from(Room room, List<RoomPlayer> activePlayers) {
        List<RoomPlayerResponse> playerResponses = activePlayers.stream()
                .map(rp -> RoomPlayerResponse.from(rp, room.getHostUserId()))
                .toList();

        return new RoomDetailResponse(
                room.getId().toString(),
                room.getRoomName(),
                room.getGameType(),
                room.getLanguage(),
                room.getMaxPlayers(),
                playerResponses
        );
    }
}
