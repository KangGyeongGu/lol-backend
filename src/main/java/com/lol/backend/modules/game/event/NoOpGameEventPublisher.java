package com.lol.backend.modules.game.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * NoOp 구현체.
 * 실제 구현체(StompGameEventPublisher 등)가 없을 때 폴백으로 사용됨.
 * 모든 이벤트 발행 호출을 로깅만 하고 무시함.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(GameEventPublisher.class)
public class NoOpGameEventPublisher implements GameEventPublisher {

    @Override
    public void gameStageChanged(UUID gameId, UUID roomId, String gameType, String stage,
                                 String stageStartedAt, String stageDeadlineAt, long remainingMs) {
        log.debug("[NoOp] gameStageChanged: gameId={}, stage={}, remainingMs={}", gameId, stage, remainingMs);
    }

    @Override
    public void gameStageChanged(UUID gameId, UUID roomId, String gameType, String stage,
                                 String stageStartedAt, String stageDeadlineAt, long remainingMs,
                                 java.time.Instant serverTime) {
        log.debug("[NoOp] gameStageChanged (SSOT): gameId={}, stage={}, remainingMs={}, serverTime={}",
                gameId, stage, remainingMs, serverTime.toString());
    }

    @Override
    public void gameBanSubmitted(UUID gameId, UUID roomId, UUID userId, UUID algorithmId, String submittedAt) {
        log.debug("[NoOp] gameBanSubmitted: gameId={}, userId={}, algorithmId={}", gameId, userId, algorithmId);
    }

    @Override
    public void gamePickSubmitted(UUID gameId, UUID roomId, UUID userId, UUID algorithmId, String submittedAt) {
        log.debug("[NoOp] gamePickSubmitted: gameId={}, userId={}, algorithmId={}", gameId, userId, algorithmId);
    }

    @Override
    public void gameItemPurchased(UUID gameId, UUID roomId, UUID userId, UUID itemId,
                                  int quantity, int unitPrice, int totalPrice, String purchasedAt) {
        log.debug("[NoOp] gameItemPurchased: gameId={}, userId={}, itemId={}, quantity={}, totalPrice={}",
                gameId, userId, itemId, quantity, totalPrice);
    }

    @Override
    public void gameSpellPurchased(UUID gameId, UUID roomId, UUID userId, UUID spellId,
                                   int quantity, int unitPrice, int totalPrice, String purchasedAt) {
        log.debug("[NoOp] gameSpellPurchased: gameId={}, userId={}, spellId={}, quantity={}, totalPrice={}",
                gameId, userId, spellId, quantity, totalPrice);
    }

    @Override
    public void gameFinished(UUID gameId, UUID roomId, String finishedAt, java.util.List<GameEventPublisher.GameFinishedResultData> results) {
        log.debug("[NoOp] gameFinished: gameId={}, resultsCount={}", gameId, results.size());
    }
}
