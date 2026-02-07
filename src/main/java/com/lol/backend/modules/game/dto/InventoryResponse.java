package com.lol.backend.modules.game.dto;

import java.util.List;

/**
 * 인벤토리 응답 DTO.
 * OPENAPI Inventory 스키마 참조.
 */
public record InventoryResponse(
        List<InventoryItemResponse> items,
        List<InventorySpellResponse> spells
) {
    public static InventoryResponse empty() {
        return new InventoryResponse(List.of(), List.of());
    }
}
