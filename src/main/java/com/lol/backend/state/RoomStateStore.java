package com.lol.backend.state;

import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomStateStore {

    void saveRoom(RoomStateDto room);

    Optional<RoomStateDto> getRoom(UUID roomId);

    void deleteRoom(UUID roomId);

    List<RoomStateDto> getAllActiveRooms();

    void addPlayer(RoomPlayerStateDto player);

    void removePlayer(UUID roomId, UUID userId);

    Optional<RoomPlayerStateDto> getPlayer(UUID roomId, UUID userId);

    List<RoomPlayerStateDto> getPlayers(UUID roomId);

    void updatePlayerState(UUID roomId, UUID userId, String state);

    void incrementListVersion();

    long getListVersion();
}
