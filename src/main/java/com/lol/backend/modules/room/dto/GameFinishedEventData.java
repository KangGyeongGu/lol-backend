package com.lol.backend.modules.room.dto;

import java.util.List;

/**
 * GAME_FINISHED 이벤트 payload (SSOT EVENTS.md 5.2 기준).
 * Topic: /topic/games/{gameId}
 *
 * @param gameId 게임 ID
 * @param roomId 룸 ID
 * @param finishedAt 게임 종료 시각 (ISO-8601)
 * @param results 게임 결과 목록
 */
public record GameFinishedEventData(
        String gameId,
        String roomId,
        String finishedAt,
        List<GameResultData> results
) {
    /**
     * 게임 결과 개별 플레이어 데이터.
     *
     * @param userId 유저 ID
     * @param nickname 닉네임
     * @param result 게임 결과 (WIN, LOSE, DRAW)
     * @param rankInGame 게임 내 순위
     * @param scoreDelta 점수 변동
     * @param coinDelta 코인 변동
     * @param expDelta 경험치 변동
     * @param finalScoreValue 최종 점수
     * @param solved 문제 해결 여부
     */
    public record GameResultData(
            String userId,
            String nickname,
            String result,
            int rankInGame,
            int scoreDelta,
            int coinDelta,
            double expDelta,
            int finalScoreValue,
            boolean solved
    ) {
    }
}
