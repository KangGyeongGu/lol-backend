package com.lol.backend.modules.room.repo;

import com.lol.backend.modules.room.entity.RoomHostHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomHostHistoryRepository extends JpaRepository<RoomHostHistory, UUID> {

    void deleteAllByRoomId(UUID roomId);
}
