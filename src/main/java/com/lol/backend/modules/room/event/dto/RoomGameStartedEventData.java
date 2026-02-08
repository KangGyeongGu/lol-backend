package com.lol.backend.modules.room.event.dto;

/**
 * ROOM_GAME_STARTED 이벤트 데이터.
 * EVENTS.md 5.6 기준.
 */
public record RoomGameStartedEventData(
    String roomId,
    String gameId,
    String gameType,
    String stage,
    String pageRoute,
    String stageStartedAt,
    String stageDeadlineAt,
    long remainingMs
) {}
