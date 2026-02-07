package com.lol.backend.modules.room.event;

import java.util.UUID;

public interface RoomEventPublisher {

    void roomCreated(UUID roomId);

    void roomRemoved(UUID roomId);

    void roomUpdated(UUID roomId);

    void playerJoined(UUID roomId, UUID userId);

    void playerLeft(UUID roomId, UUID userId);

    void playerStateChanged(UUID roomId, UUID userId, String newState);

    void hostChanged(UUID roomId, UUID newHostUserId);

    void playerKicked(UUID roomId, UUID kickedUserId);

    void gameStarted(UUID roomId, UUID gameId);

    long getListVersion();
}
