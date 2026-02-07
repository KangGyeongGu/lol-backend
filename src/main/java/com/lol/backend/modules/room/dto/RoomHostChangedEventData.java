package com.lol.backend.modules.room.dto;

/**
 * ROOM_HOST_CHANGED 이벤트 데이터.
 * EVENTS.md 4.4 기준.
 */
public record RoomHostChangedEventData(
    String roomId,
    String fromUserId,
    String toUserId,
    String reason,
    String changedAt
) {}
