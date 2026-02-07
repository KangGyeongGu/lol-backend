package com.lol.backend.modules.room.dto;

/**
 * ROOM_LIST_REMOVED 이벤트 데이터.
 * EVENTS.md 2.2 기준.
 */
public record RoomListRemovedEventData(
    String roomId,
    long listVersion,
    String reason
) {}
