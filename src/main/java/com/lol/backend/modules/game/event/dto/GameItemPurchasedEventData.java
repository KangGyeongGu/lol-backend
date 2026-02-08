package com.lol.backend.modules.game.event.dto;

/**
 * GAME_ITEM_PURCHASED 이벤트 payload (SSOT EVENTS.md 5.4 기준).
 * Topic: /topic/games/{gameId}
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
public record GameItemPurchasedEventData(
        String gameId,
        String roomId,
        String userId,
        String itemId,
        int quantity,
        int unitPrice,
        int totalPrice,
        String purchasedAt
) {
}
