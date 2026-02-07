package com.lol.backend.modules.room.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class NoOpRoomEventPublisher implements RoomEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpRoomEventPublisher.class);
    private final AtomicLong listVersion = new AtomicLong(0);

    @Override
    public void roomCreated(UUID roomId) {
        listVersion.incrementAndGet();
        log.debug("[NoOp] roomCreated: roomId={}", roomId);
    }

    @Override
    public void roomRemoved(UUID roomId) {
        listVersion.incrementAndGet();
        log.debug("[NoOp] roomRemoved: roomId={}", roomId);
    }

    @Override
    public void roomUpdated(UUID roomId) {
        listVersion.incrementAndGet();
        log.debug("[NoOp] roomUpdated: roomId={}", roomId);
    }

    @Override
    public void playerJoined(UUID roomId, UUID userId) {
        listVersion.incrementAndGet();
        log.debug("[NoOp] playerJoined: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public void playerLeft(UUID roomId, UUID userId) {
        listVersion.incrementAndGet();
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
        listVersion.incrementAndGet();
        log.debug("[NoOp] playerKicked: roomId={}, kickedUser={}", roomId, kickedUserId);
    }

    @Override
    public void gameStarted(UUID roomId, UUID gameId) {
        listVersion.incrementAndGet();
        log.debug("[NoOp] gameStarted: roomId={}, gameId={}", roomId, gameId);
    }

    @Override
    public long getListVersion() {
        return listVersion.get();
    }
}
