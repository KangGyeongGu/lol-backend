package com.lol.backend.modules.room.repo;

import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.user.entity.Language;

import java.time.Instant;
import java.util.List;

public interface RoomRepositoryCustom {

    List<Room> findRoomsWithFilters(String roomName,
                                    Language language,
                                    GameType gameType,
                                    Instant cursorUpdatedAt,
                                    int limit);
}
