package com.lol.backend.modules.room.repo;

import com.lol.backend.modules.room.entity.RoomKick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomKickRepository extends JpaRepository<RoomKick, UUID> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    void deleteAllByRoomId(UUID roomId);
}
