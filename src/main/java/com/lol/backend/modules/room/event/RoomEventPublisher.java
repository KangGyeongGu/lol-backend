package com.lol.backend.modules.room.event;

import com.lol.backend.modules.room.dto.RoomSummaryResponse;
import java.util.UUID;

/**
 * 룸/대기실 실시간 이벤트 발행 인터페이스.
 * SSOT 이벤트 명세(03_API/PAGE_MAP/ROOM_LIST.md, WAITING_ROOM.md)에 따라 정의됨.
 */
public interface RoomEventPublisher {

    /**
     * ROOM_LIST_UPSERT: /topic/rooms/list
     * 방 생성/상태변경/인원변동 시 호출.
     *
     * @param room 룸 요약 정보
     * @param listVersion 룸 목록 버전
     */
    void roomListUpsert(RoomSummaryResponse room, long listVersion);

    /**
     * ROOM_LIST_REMOVED: /topic/rooms/list
     * 방 삭제 시 호출.
     *
     * @param roomId 룸 ID
     * @param listVersion 룸 목록 버전
     * @param reason 삭제 사유 (GAME_STARTED, DISBANDED 등)
     */
    void roomListRemoved(UUID roomId, long listVersion, String reason);

    /**
     * ROOM_PLAYER_JOINED: /topic/rooms/{roomId}/lobby
     * 플레이어 참가 시 호출.
     *
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param nickname 닉네임
     * @param state 플레이어 상태 (READY, UNREADY 등)
     * @param joinedAt 참가 시각 (ISO-8601)
     */
    void playerJoined(UUID roomId, UUID userId, String nickname, String state, String joinedAt);

    /**
     * ROOM_PLAYER_LEFT: /topic/rooms/{roomId}/lobby
     * 플레이어 퇴장 시 호출.
     *
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param leftAt 퇴장 시각 (ISO-8601)
     * @param reason 퇴장 사유 (LEAVE, KICKED 등)
     */
    void playerLeft(UUID roomId, UUID userId, String leftAt, String reason);

    /**
     * ROOM_PLAYER_STATE_CHANGED: /topic/rooms/{roomId}/lobby
     * 플레이어 상태 변경 시 호출.
     *
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param state 새 상태 (READY, UNREADY, DISCONNECTED 등)
     * @param updatedAt 변경 시각 (ISO-8601)
     */
    void playerStateChanged(UUID roomId, UUID userId, String state, String updatedAt);

    /**
     * ROOM_HOST_CHANGED: /topic/rooms/{roomId}/lobby
     * 방장 변경 시 호출.
     *
     * @param roomId 룸 ID
     * @param fromUserId 이전 방장 ID (null 가능)
     * @param toUserId 새 방장 ID
     * @param reason 변경 사유 (HOST_LEFT, KICKED, TRANSFERRED 등)
     * @param changedAt 변경 시각 (ISO-8601)
     */
    void hostChanged(UUID roomId, UUID fromUserId, UUID toUserId, String reason, String changedAt);

    /**
     * ROOM_KICKED: /user/queue/rooms
     * 플레이어 강퇴 시 개인 알림.
     *
     * @param roomId 룸 ID
     * @param kickedUserId 강퇴당한 유저 ID
     * @param kickedByUserId 강퇴 실행 유저 ID
     * @param kickedAt 강퇴 시각 (ISO-8601)
     */
    void playerKicked(UUID roomId, UUID kickedUserId, UUID kickedByUserId, String kickedAt);

    /**
     * ROOM_GAME_STARTED: /topic/rooms/{roomId}/lobby
     * 게임 시작 시 호출 (SSOT EVENTS.md 5.6 기준).
     * 대기실 클라이언트에게 gameId를 전달하여 /topic/games/{gameId} 구독 전환을 트리거한다.
     *
     * @param roomId 룸 ID
     * @param gameId 게임 ID
     * @param gameType 게임 타입 (NORMAL, RANKED)
     * @param stage 첫 stage (BAN, PLAY 등 — LOBBY 제외)
     * @param pageRoute 페이지 라우트 (BAN_PICK_SHOP, IN_GAME)
     * @param stageStartedAt stage 시작 시각 (ISO-8601)
     * @param stageDeadlineAt stage 마감 시각 (ISO-8601)
     * @param remainingMs 남은 시간(ms)
     */
    void roomGameStarted(UUID roomId, UUID gameId, String gameType, String stage,
                         String pageRoute, String stageStartedAt, String stageDeadlineAt, long remainingMs);
}
