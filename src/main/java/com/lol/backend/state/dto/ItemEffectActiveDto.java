package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 활성 효과 DTO (아이템/스펠 공통).
 * effectType을 통해 ITEM/SPELL을 구분한다.
 */
public record ItemEffectActiveDto(
        UUID gameId,
        UUID userId,
        UUID itemId, // effectType이 SPELL인 경우 spellId가 저장됨
        String uniqueId,
        Instant startedAt,
        Instant expiresAt,
        String effectType // "ITEM" or "SPELL"
) {
}
