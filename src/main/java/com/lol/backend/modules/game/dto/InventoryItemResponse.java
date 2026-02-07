package com.lol.backend.modules.game.dto;

/**
 * 인벤토리 아이템 응답 DTO.
 * OPENAPI InventoryItem 스키마 참조.
 */
public record InventoryItemResponse(
        String itemId,
        int quantity
) {
}
