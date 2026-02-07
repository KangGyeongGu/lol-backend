package com.lol.backend.modules.room.event;

import com.lol.backend.state.RoomStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class NoOpRoomEventPublisher implements RoomEventPublisher {

    private final RoomStateStore roomStateStore;

    public NoOpRoomEventPublisher(RoomStateStore roomStateStore) {
        this.roomStateStore = roomStateStore;
    }

    @Override
    public void roomCreated(UUID roomId) {
        log.debug("[NoOp] roomCreated: roomId={}", roomId);
    }

    @Override
    public void roomRemoved(UUID roomId) {
        log.debug("[NoOp] roomRemoved: roomId={}", roomId);
    }

    @Override
    public void roomUpdated(UUID roomId) {
        log.debug("[NoOp] roomUpdated: roomId={}", roomId);
    }

    @Override
    public void playerJoined(UUID roomId, UUID userId) {
        log.debug("[NoOp] playerJoined: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public void playerLeft(UUID roomId, UUID userId) {
        log.debug("[NoOp] playerLeft: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public void playerStateChanged(UUID roomId, UUID userId, String newState) {
        log.debug("[NoOp] playerStateChanged: roomId={}, userId={}, state={}", roomId, userId, newState);
    }

    @Override
    public void hostChanged(UUID roomId, UUID newHostUserId) {
        log.debug("[NoOp] hostChanged: roomId={}, newHost={}", roomId, newHostUserId);
    }

    @Override
    public void playerKicked(UUID roomId, UUID kickedUserId) {
        log.debug("[NoOp] playerKicked: roomId={}, kickedUser={}", roomId, kickedUserId);
    }

    @Override
    public void gameStarted(UUID roomId, UUID gameId) {
        log.debug("[NoOp] gameStarted: roomId={}, gameId={}", roomId, gameId);
    }

    @Override
    public long getListVersion() {
        return roomStateStore.getListVersion();
    }
}
