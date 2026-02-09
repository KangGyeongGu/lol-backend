package com.lol.backend.modules.game.event;

import com.lol.backend.modules.game.event.dto.GameStageChangedEventData;
import com.lol.backend.modules.game.event.dto.GameBanSubmittedEventData;
import com.lol.backend.modules.game.event.dto.GamePickSubmittedEventData;
import com.lol.backend.modules.game.event.dto.GameItemPurchasedEventData;
import com.lol.backend.modules.game.event.dto.GameSpellPurchasedEventData;
import com.lol.backend.modules.game.event.dto.GameFinishedEventData;
import com.lol.backend.realtime.dto.EventType;
import com.lol.backend.realtime.support.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * STOMP 기반 GameEventPublisher 구현체.
 * EventPublisher(SimpMessagingTemplate 래퍼)를 통해 실제 WebSocket 이벤트를 전파한다.
 * NoOpGameEventPublisher는 @ConditionalOnMissingBean으로 이 빈이 있으면 비활성화된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompGameEventPublisher implements GameEventPublisher {

    private static final String TOPIC_GAME = "/topic/games/%s";

    private final EventPublisher eventPublisher;

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
    public void gameStageChanged(UUID gameId, UUID roomId, String gameType, String stage,
                                 String stageStartedAt, String stageDeadlineAt, long remainingMs,
                                 java.time.Instant serverTime) {
        var data = new GameStageChangedEventData(
                gameId.toString(),
                roomId.toString(),
                gameType,
                stage,
                stageStartedAt,
                stageDeadlineAt,
                remainingMs
        );
        // SSOT 계약 준수: meta.serverTime과 remainingMs를 동일 Instant 기반으로 생성
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_STAGE_CHANGED, data, serverTime);
        log.debug("gameStageChanged (SSOT): gameId={}, stage={}, remainingMs={}, serverTime={}",
                gameId, stage, remainingMs, serverTime.toString());
    }

    @Override
    public void gameBanSubmitted(UUID gameId, UUID roomId, UUID userId, UUID algorithmId, String submittedAt) {
        var data = new GameBanSubmittedEventData(
                gameId.toString(),
                roomId.toString(),
                userId.toString(),
                algorithmId.toString(),
                submittedAt
        );
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_BAN_SUBMITTED, data);
        log.debug("gameBanSubmitted: gameId={}, userId={}, algorithmId={}", gameId, userId, algorithmId);
    }

    @Override
    public void gamePickSubmitted(UUID gameId, UUID roomId, UUID userId, UUID algorithmId, String submittedAt) {
        var data = new GamePickSubmittedEventData(
                gameId.toString(),
                roomId.toString(),
                userId.toString(),
                algorithmId.toString(),
                submittedAt
        );
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_PICK_SUBMITTED, data);
        log.debug("gamePickSubmitted: gameId={}, userId={}, algorithmId={}", gameId, userId, algorithmId);
    }

    @Override
    public void gameItemPurchased(UUID gameId, UUID roomId, UUID userId, UUID itemId,
                                  int quantity, int unitPrice, int totalPrice, String purchasedAt) {
        var data = new GameItemPurchasedEventData(
                gameId.toString(),
                roomId.toString(),
                userId.toString(),
                itemId.toString(),
                quantity,
                unitPrice,
                totalPrice,
                purchasedAt
        );
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_ITEM_PURCHASED, data);
        log.debug("gameItemPurchased: gameId={}, userId={}, itemId={}, quantity={}, totalPrice={}",
                gameId, userId, itemId, quantity, totalPrice);
    }

    @Override
    public void gameSpellPurchased(UUID gameId, UUID roomId, UUID userId, UUID spellId,
                                   int quantity, int unitPrice, int totalPrice, String purchasedAt) {
        var data = new GameSpellPurchasedEventData(
                gameId.toString(),
                roomId.toString(),
                userId.toString(),
                spellId.toString(),
                quantity,
                unitPrice,
                totalPrice,
                purchasedAt
        );
        eventPublisher.broadcast(gameTopic(gameId), EventType.GAME_SPELL_PURCHASED, data);
        log.debug("gameSpellPurchased: gameId={}, userId={}, spellId={}, quantity={}, totalPrice={}",
                gameId, userId, spellId, quantity, totalPrice);
    }

    @Override
    public void gameFinished(UUID gameId, UUID roomId, String finishedAt, java.util.List<GameEventPublisher.GameFinishedResultData> results) {
        var resultList = results.stream()
                .map(r -> new GameFinishedEventData.GameResultData(
                        r.userId().toString(),
                        r.nickname(),
                        r.result(),
                        r.rankInGame(),
                        r.scoreDelta(),
                        r.coinBefore(),
                        r.coinDelta(),
                        r.expBefore(),
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

    private String gameTopic(UUID gameId) {
        return String.format(TOPIC_GAME, gameId);
    }
}
