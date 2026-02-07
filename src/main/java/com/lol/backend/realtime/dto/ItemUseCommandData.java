package com.lol.backend.realtime.dto;

/**
 * ITEM_USE 커맨드 데이터.
 */
public record ItemUseCommandData(
    String itemId,
    String targetUserId
) {}
