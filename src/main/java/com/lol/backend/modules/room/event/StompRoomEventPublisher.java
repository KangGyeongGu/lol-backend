package com.lol.backend.modules.room.event;

import com.lol.backend.modules.room.dto.*;
import com.lol.backend.realtime.dto.EventType;
import com.lol.backend.realtime.support.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * STOMP 기반 RoomEventPublisher 구현체.
 * EventPublisher(SimpMessagingTemplate 래퍼)를 통해 실제 WebSocket 이벤트를 전파한다.
 * NoOpRoomEventPublisher는 @ConditionalOnMissingBean으로 이 빈이 있으면 비활성화된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompRoomEventPublisher implements RoomEventPublisher {

    private static final String TOPIC_ROOM_LIST = "/topic/rooms/list";
    private static final String TOPIC_ROOM_LOBBY = "/topic/rooms/%s/lobby";
    private static final String TOPIC_GAME = "/topic/games/%s";
    private static final String QUEUE_ROOMS = "/queue/rooms";

    private final EventPublisher eventPublisher;

    @Override
    public void roomListUpsert(RoomSummaryResponse room, long listVersion) {
        var data = new RoomListUpsertEventData(room, listVersion);
        eventPublisher.broadcast(TOPIC_ROOM_LIST, EventType.ROOM_LIST_UPSERT, data);
        log.debug("roomListUpsert: roomId={}, listVersion={}", room.roomId(), listVersion);
    }

    @Override
    public void roomListRemoved(UUID roomId, long listVersion, String reason) {
        var data = new RoomListRemovedEventData(roomId.toString(), listVersion, reason);
        eventPublisher.broadcast(TOPIC_ROOM_LIST, EventType.ROOM_LIST_REMOVED, data);
        log.debug("roomListRemoved: roomId={}, listVersion={}, reason={}", roomId, listVersion, reason);
    }

    @Override
    public void playerJoined(UUID roomId, UUID userId, String nickname, String state, String joinedAt) {
        var data = new RoomPlayerJoinedEventData(roomId.toString(), userId.toString(), nickname, state, joinedAt);
        eventPublisher.broadcast(lobbyTopic(roomId), EventType.ROOM_PLAYER_JOINED, data);
        log.debug("playerJoined: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public void playerLeft(UUID roomId, UUID userId, String leftAt, String reason) {
        var data = new RoomPlayerLeftEventData(roomId.toString(), userId.toString(), leftAt, reason);
        eventPublisher.broadcast(lobbyTopic(roomId), EventType.ROOM_PLAYER_LEFT, data);
        log.debug("playerLeft: roomId={}, userId={}, reason={}", roomId, userId, reason);
    }

    @Override
    public void playerStateChanged(UUID roomId, UUID userId, String state, String updatedAt) {
        var data = new RoomPlayerStateChangedEventData(roomId.toString(), userId.toString(), state, updatedAt);
        eventPublisher.broadcast(lobbyTopic(roomId), EventType.ROOM_PLAYER_STATE_CHANGED, data);
        log.debug("playerStateChanged: roomId={}, userId={}, state={}", roomId, userId, state);
    }

    @Override
    public void hostChanged(UUID roomId, UUID fromUserId, UUID toUserId, String reason, String changedAt) {
        var data = new RoomHostChangedEventData(
                roomId.toString(),
                fromUserId != null ? fromUserId.toString() : null,
                toUserId.toString(),
                reason,
                changedAt
        );
        eventPublisher.broadcast(lobbyTopic(roomId), EventType.ROOM_HOST_CHANGED, data);
        log.debug("hostChanged: roomId={}, from={}, to={}, reason={}", roomId, fromUserId, toUserId, reason);
    }

    @Override
    public void playerKicked(UUID roomId, UUID kickedUserId, UUID kickedByUserId, String kickedAt) {
        var data = new RoomKickedEventData(roomId.toString(), kickedByUserId.toString(), kickedAt);
        eventPublisher.sendToUser(kickedUserId.toString(), QUEUE_ROOMS, EventType.ROOM_KICKED, data);
        log.debug("playerKicked: roomId={}, kickedUserId={}, by={}", roomId, kickedUserId, kickedByUserId);
    }

    @Override
    @Deprecated
    public void gameStarted(UUID roomId, UUID gameId) {
        log.warn("gameStarted() is deprecated. Use gameStageChanged() instead. roomId={}, gameId={}", roomId, gameId);
        // 하위 호환성 유지: 게임 시작 시 LOBBY stage로 알림 (실제로는 gameStageChanged 호출 권장)
        gameStageChanged(gameId, roomId, "RANKED", "LOBBY", null, null, 0L);
    }

    @Override
    public void gameStageChanged(UUID gameId, UUID roomId, String gameType, String stage,
                                 String stageStartedAt, String stageDeadlineAt, long remainingMs) {
        var data = new GameStageChangedEventData(
                gameId.toString(),
                roomId.toString(),
                gameType,
                stage,
                stageStartedAt,
                stageDeadlineAt,
                remainingMs
        );
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_STAGE_CHANGED, data);
        log.debug("gameStageChanged: gameId={}, stage={}, remainingMs={}", gameId, stage, remainingMs);
    }

    @Override
    public void gameFinished(UUID gameId, UUID roomId, String finishedAt, java.util.List<RoomEventPublisher.GameFinishedResultData> results) {
        var resultList = results.stream()
                .map(r -> new GameFinishedEventData.GameResultData(
                        r.userId().toString(),
                        r.nickname(),
                        r.result(),
                        r.rankInGame(),
                        r.scoreDelta(),
                        r.coinDelta(),
                        r.expDelta(),
                        r.finalScoreValue(),
                        r.solved()
                ))
                .toList();

        var data = new GameFinishedEventData(
                gameId.toString(),
                roomId.toString(),
                finishedAt,
                resultList
        );
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_FINISHED, data);
        log.debug("gameFinished: gameId={}, resultsCount={}", gameId, results.size());
    }

    private String lobbyTopic(UUID roomId) {
        return String.format(TOPIC_ROOM_LOBBY, roomId);
    }

    private String gameTopic(UUID gameId) {
        return String.format(TOPIC_GAME, gameId);
    }
}
