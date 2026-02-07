package com.lol.backend.modules.game.dto;

/**
 * 게임 내 플레이어 정보 응답 DTO.
 * OPENAPI GamePlayer 스키마 참조.
 */
public record GamePlayerResponse(
        String userId,
        String nickname,
        int score
) {
}
