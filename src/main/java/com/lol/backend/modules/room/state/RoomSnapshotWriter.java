package com.lol.backend.modules.room.state;

import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.room.repo.RoomPlayerRepository;
import com.lol.backend.modules.room.repo.RoomRepository;
import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.SnapshotWriter;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class RoomSnapshotWriter implements SnapshotWriter {

    private final RoomStateStore roomStateStore;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;

    public RoomSnapshotWriter(RoomStateStore roomStateStore,
                              RoomRepository roomRepository,
                              RoomPlayerRepository roomPlayerRepository) {
        this.roomStateStore = roomStateStore;
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
    }

    @Override
    @Transactional
    public void flushRoom(UUID roomId) {
        log.info("Flushing room snapshot to DB: roomId={}", roomId);

        Optional<RoomStateDto> roomStateOpt = roomStateStore.getRoom(roomId);
        if (roomStateOpt.isEmpty()) {
            log.warn("Room state not found in Redis: roomId={}", roomId);
            return;
        }

        RoomStateDto roomState = roomStateOpt.get();

        // Room 엔티티 조회 후 업데이트 (이미 존재하는 경우)
        Optional<Room> existingRoom = roomRepository.findById(roomId);
        if (existingRoom.isPresent()) {
            Room room = existingRoom.get();
            room.setHostUserId(roomState.hostUserId());
            roomRepository.save(room);
            log.debug("Updated room in DB: roomId={}", roomId);
        } else {
            log.warn("Room not found in DB during snapshot flush: roomId={}", roomId);
        }

        // RoomPlayer 상태 동기화
        List<RoomPlayerStateDto> players = roomStateStore.getPlayers(roomId);
        for (RoomPlayerStateDto playerState : players) {
            Optional<RoomPlayer> existingPlayer = roomPlayerRepository.findById(playerState.id());
            if (existingPlayer.isPresent()) {
                RoomPlayer player = existingPlayer.get();
                // leftAt 필드 동기화 (Redis에서 퇴장 처리된 경우)
                if (playerState.leftAt() != null && player.getLeftAt() == null) {
                    player.leave();
                }
                roomPlayerRepository.save(player);
                log.debug("Updated room player in DB: playerId={}, userId={}", playerState.id(), playerState.userId());
            } else {
                log.warn("RoomPlayer not found in DB during snapshot flush: playerId={}", playerState.id());
            }
        }

        // Redis 상태 삭제
        roomStateStore.deleteRoom(roomId);
        log.info("Completed room snapshot flush and cleared Redis state: roomId={}", roomId);
    }

    @Override
    public void flushGame(UUID gameId) {
        // Game snapshot flush는 T1-2에서 구현
        log.debug("Game snapshot flush not implemented yet: gameId={}", gameId);
    }
}
