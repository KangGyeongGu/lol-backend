package com.lol.backend.modules.game.event;

import java.util.UUID;

/**
 * 게임 실시간 이벤트 발행 인터페이스.
 * SSOT 이벤트 명세(03_API/EVENTS.md)에 따라 정의됨.
 */
public interface GameEventPublisher {

    /**
     * GAME_STAGE_CHANGED: /topic/games/{gameId}
     * 게임 stage 전이 시 호출 (SSOT EVENTS.md 5.1 기준).
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param gameType 게임 타입 (NORMAL, RANKED)
     * @param stage 현재 stage (LOBBY, BAN, PICK, SHOP, PLAY, FINISHED)
     * @param stageStartedAt stage 시작 시각 (ISO-8601)
     * @param stageDeadlineAt stage 마감 시각 (ISO-8601, null 가능)
     * @param remainingMs 남은 시간(ms)
     */
    void gameStageChanged(UUID gameId, UUID roomId, String gameType, String stage,
                          String stageStartedAt, String stageDeadlineAt, long remainingMs);

    /**
     * GAME_STAGE_CHANGED: /topic/games/{gameId}
     * 게임 stage 전이 시 호출 (SSOT EVENTS.md 5.1 기준).
     * SSOT 계약 준수: remainingMs와 meta.serverTime을 동일 Instant 기반으로 계산한다.
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param gameType 게임 타입 (NORMAL, RANKED)
     * @param stage 현재 stage (LOBBY, BAN, PICK, SHOP, PLAY, FINISHED)
     * @param stageStartedAt stage 시작 시각 (ISO-8601)
     * @param stageDeadlineAt stage 마감 시각 (ISO-8601, null 가능)
     * @param remainingMs 남은 시간(ms)
     * @param serverTime 서버 시간 (Instant, meta.serverTime과 동일)
     */
    void gameStageChanged(UUID gameId, UUID roomId, String gameType, String stage,
                          String stageStartedAt, String stageDeadlineAt, long remainingMs,
                          java.time.Instant serverTime);

    /**
     * GAME_BAN_SUBMITTED: /topic/games/{gameId}
     * 밴 제출 시 호출 (SSOT EVENTS.md 5.2 기준).
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param algorithmId 밴한 알고리즘 ID
     * @param submittedAt 제출 시각 (ISO-8601)
     */
    void gameBanSubmitted(UUID gameId, UUID roomId, UUID userId, UUID algorithmId, String submittedAt);

    /**
     * GAME_PICK_SUBMITTED: /topic/games/{gameId}
     * 픽 제출 시 호출 (SSOT EVENTS.md 5.3 기준).
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param algorithmId 픽한 알고리즘 ID
     * @param submittedAt 제출 시각 (ISO-8601)
     */
    void gamePickSubmitted(UUID gameId, UUID roomId, UUID userId, UUID algorithmId, String submittedAt);

    /**
     * GAME_ITEM_PURCHASED: /topic/games/{gameId}
     * 아이템 구매 시 호출 (SSOT EVENTS.md 5.4 기준).
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param itemId 아이템 ID
     * @param quantity 구매 수량
     * @param unitPrice 단가
     * @param totalPrice 총액
     * @param purchasedAt 구매 시각 (ISO-8601)
     */
    void gameItemPurchased(UUID gameId, UUID roomId, UUID userId, UUID itemId,
                           int quantity, int unitPrice, int totalPrice, String purchasedAt);

    /**
     * GAME_SPELL_PURCHASED: /topic/games/{gameId}
     * 스펠 구매 시 호출 (SSOT EVENTS.md 5.5 기준).
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param userId 유저 ID
     * @param spellId 스펠 ID
     * @param quantity 구매 수량
     * @param unitPrice 단가
     * @param totalPrice 총액
     * @param purchasedAt 구매 시각 (ISO-8601)
     */
    void gameSpellPurchased(UUID gameId, UUID roomId, UUID userId, UUID spellId,
                            int quantity, int unitPrice, int totalPrice, String purchasedAt);

    /**
     * GAME_FINISHED: /topic/games/{gameId}
     * 게임 종료 시 호출 (SSOT EVENTS.md 5.6 기준).
     *
     * @param gameId 게임 ID
     * @param roomId 룸 ID
     * @param finishedAt 종료 시각 (ISO-8601)
     * @param results 게임 결과 목록 (userId → nickname, result, rank, deltas 등)
     */
    void gameFinished(UUID gameId, UUID roomId, String finishedAt, java.util.List<GameFinishedResultData> results);

    /**
     * 게임 결과 개별 플레이어 데이터.
     */
    record GameFinishedResultData(
            UUID userId,
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
