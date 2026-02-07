package com.lol.backend.modules.room.dto;

import com.lol.backend.modules.room.entity.PlayerState;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.user.dto.UserSummaryResponse;

import java.util.UUID;

public record RoomPlayerResponse(
        UserSummaryResponse user,
        PlayerState state,
        boolean isHost
) {
    public static RoomPlayerResponse from(RoomPlayer roomPlayer, UUID hostUserId) {
        return new RoomPlayerResponse(
                UserSummaryResponse.from(roomPlayer.getUser()),
                roomPlayer.getState(),
                roomPlayer.getUserId().equals(hostUserId)
        );
    }
}
