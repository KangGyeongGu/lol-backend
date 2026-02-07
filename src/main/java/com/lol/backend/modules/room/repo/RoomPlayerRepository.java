package com.lol.backend.modules.room.repo;

import com.lol.backend.modules.room.entity.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, UUID> {

    @Query("SELECT rp FROM RoomPlayer rp JOIN FETCH rp.user WHERE rp.roomId = :roomId AND rp.leftAt IS NULL")
    List<RoomPlayer> findActivePlayersByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT rp FROM RoomPlayer rp WHERE rp.roomId = :roomId AND rp.userId = :userId AND rp.leftAt IS NULL")
    Optional<RoomPlayer> findActivePlayer(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(rp) FROM RoomPlayer rp WHERE rp.roomId = :roomId AND rp.leftAt IS NULL")
    int countActivePlayersByRoomId(@Param("roomId") UUID roomId);

    void deleteAllByRoomId(UUID roomId);
}
