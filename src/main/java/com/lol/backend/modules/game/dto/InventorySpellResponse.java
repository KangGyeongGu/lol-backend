package com.lol.backend.modules.game.dto;

/**
 * 인벤토리 스펠 응답 DTO.
 * OPENAPI InventorySpell 스키마 참조.
 */
public record InventorySpellResponse(
        String spellId,
        int quantity
) {
}
