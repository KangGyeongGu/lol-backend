package com.lol.backend.modules.room.event.dto;

import com.lol.backend.modules.room.dto.RoomSummaryResponse;

/**
 * ROOM_LIST_UPSERT 이벤트 데이터.
 * EVENTS.md 2.1 기준.
 */
public record RoomListUpsertEventData(
    RoomSummaryResponse room,
    long listVersion
) {}
