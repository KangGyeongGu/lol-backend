package com.lol.backend.modules.game.event.dto;

/**
 * GAME_BAN_SUBMITTED 이벤트 payload (SSOT EVENTS.md 5.2 기준).
 * Topic: /topic/games/{gameId}
 *
 * @param gameId 게임 ID
 * @param roomId 룸 ID
 * @param userId 유저 ID
 * @param algorithmId 밴한 알고리즘 ID
 * @param submittedAt 제출 시각 (ISO-8601)
 */
public record GameBanSubmittedEventData(
        String gameId,
        String roomId,
        String userId,
        String algorithmId,
        String submittedAt
) {
}
