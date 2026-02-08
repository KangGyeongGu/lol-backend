package com.lol.backend.modules.game.event.dto;

/**
 * GAME_STAGE_CHANGED 이벤트 payload (SSOT EVENTS.md 5.1 기준).
 * Topic: /topic/games/{gameId}
 *
 * @param gameId 게임 ID
 * @param roomId 룸 ID
 * @param gameType 게임 타입 (NORMAL, RANKED)
 * @param stage 게임 stage (LOBBY, BAN, PICK, SHOP, PLAY, FINISHED)
 * @param stageStartedAt stage 시작 시각 (ISO-8601)
 * @param stageDeadlineAt stage 마감 시각 (ISO-8601, null 가능)
 * @param remainingMs 남은 시간(ms)
 */
public record GameStageChangedEventData(
        String gameId,
        String roomId,
        String gameType,
        String stage,
        String stageStartedAt,
        String stageDeadlineAt,
        long remainingMs
) {
}
