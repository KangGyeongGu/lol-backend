package com.lol.backend.modules.room.event;

import com.lol.backend.modules.room.dto.RoomSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * NoOp 구현체.
 * 실제 구현체(StompRoomEventPublisher 등)가 없을 때 폴백으로 사용됨.
 * 모든 이벤트 발행 호출을 로깅만 하고 무시함.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RoomEventPublisher.class)
public class NoOpRoomEventPublisher implements RoomEventPublisher {

    @Override
    public void roomListUpsert(RoomSummaryResponse room, long listVersion) {
        log.debug("[NoOp] roomListUpsert: roomId={}, listVersion={}", room.roomId(), listVersion);
    }

    @Override
    public void roomListRemoved(UUID roomId, long listVersion, String reason) {
        log.debug("[NoOp] roomListRemoved: roomId={}, listVersion={}, reason={}", roomId, listVersion, reason);
    }

    @Override
    public void playerJoined(UUID roomId, UUID userId, String nickname, String state, String joinedAt) {
        log.debug("[NoOp] playerJoined: roomId={}, userId={}, nickname={}, state={}, joinedAt={}",
                roomId, userId, nickname, state, joinedAt);
    }

    @Override
    public void playerLeft(UUID roomId, UUID userId, String leftAt, String reason) {
        log.debug("[NoOp] playerLeft: roomId={}, userId={}, leftAt={}, reason={}",
                roomId, userId, leftAt, reason);
    }

    @Override
    public void playerStateChanged(UUID roomId, UUID userId, String state, String updatedAt) {
        log.debug("[NoOp] playerStateChanged: roomId={}, userId={}, state={}, updatedAt={}",
                roomId, userId, state, updatedAt);
    }

    @Override
    public void hostChanged(UUID roomId, UUID fromUserId, UUID toUserId, String reason, String changedAt) {
        log.debug("[NoOp] hostChanged: roomId={}, fromUserId={}, toUserId={}, reason={}, changedAt={}",
                roomId, fromUserId, toUserId, reason, changedAt);
    }

    @Override
    public void playerKicked(UUID roomId, UUID kickedUserId, UUID kickedByUserId, String kickedAt) {
        log.debug("[NoOp] playerKicked: roomId={}, kickedUserId={}, kickedByUserId={}, kickedAt={}",
                roomId, kickedUserId, kickedByUserId, kickedAt);
    }

    @Override
    public void gameStarted(UUID roomId, UUID gameId) {
        log.debug("[NoOp] gameStarted: roomId={}, gameId={}", roomId, gameId);
    }
}
