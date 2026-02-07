package com.lol.backend.modules.room.state;

import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.entity.HostChangeReason;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomHostHistory;
import com.lol.backend.modules.room.entity.RoomKick;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.room.entity.PlayerState;
import com.lol.backend.modules.room.repo.RoomHostHistoryRepository;
import com.lol.backend.modules.room.repo.RoomKickRepository;
import com.lol.backend.modules.room.repo.RoomPlayerRepository;
import com.lol.backend.modules.room.repo.RoomRepository;
import com.lol.backend.modules.user.entity.Language;
import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.SnapshotWriter;
import com.lol.backend.state.dto.RoomHostHistoryStateDto;
import com.lol.backend.state.dto.RoomKickStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RoomSnapshotWriter implements SnapshotWriter {

    private final RoomStateStore roomStateStore;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final RoomKickRepository roomKickRepository;
    private final RoomHostHistoryRepository roomHostHistoryRepository;

    public RoomSnapshotWriter(RoomStateStore roomStateStore,
                              RoomRepository roomRepository,
                              RoomPlayerRepository roomPlayerRepository,
                              RoomKickRepository roomKickRepository,
                              RoomHostHistoryRepository roomHostHistoryRepository) {
        this.roomStateStore = roomStateStore;
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.roomKickRepository = roomKickRepository;
        this.roomHostHistoryRepository = roomHostHistoryRepository;
    }

    @Override
    @Transactional
    public void persistRoom(UUID roomId) {
        log.info("Persisting room snapshot to DB: roomId={}", roomId);

        RoomStateDto roomState = roomStateStore.getRoom(roomId).orElse(null);
        if (roomState == null) {
            log.warn("Room state not found in Redis: roomId={}", roomId);
            return;
        }

        // Room upsert
        Room dbRoom = roomRepository.findById(roomId).orElse(null);
        if (dbRoom != null) {
            dbRoom.setRoomName(roomState.roomName());
            dbRoom.setHostUserId(roomState.hostUserId());
            roomRepository.save(dbRoom);
        } else {
            dbRoom = Room.restore(
                    roomState.id(),
                    roomState.roomName(),
                    GameType.valueOf(roomState.gameType()),
                    Language.valueOf(roomState.language()),
                    roomState.maxPlayers(),
                    roomState.hostUserId(),
                    roomState.createdAt(),
                    roomState.updatedAt()
            );
            roomRepository.save(dbRoom);
        }
        log.debug("Room saved to DB: roomId={}", roomId);

        // RoomPlayer upsert
        List<RoomPlayerStateDto> players = roomStateStore.getPlayers(roomId);
        for (RoomPlayerStateDto playerState : players) {
            RoomPlayer dbPlayer = roomPlayerRepository.findById(playerState.id()).orElse(null);
            if (dbPlayer != null) {
                dbPlayer.setState(PlayerState.valueOf(playerState.state()));
                if (playerState.leftAt() != null) {
                    dbPlayer.setLeftAt(playerState.leftAt());
                }
                if (playerState.disconnectedAt() != null) {
                    dbPlayer.setDisconnectedAt(playerState.disconnectedAt());
                }
                roomPlayerRepository.save(dbPlayer);
            } else {
                dbPlayer = RoomPlayer.restore(
                        playerState.id(),
                        playerState.roomId(),
                        playerState.userId(),
                        PlayerState.valueOf(playerState.state()),
                        playerState.joinedAt(),
                        playerState.leftAt(),
                        playerState.disconnectedAt()
                );
                roomPlayerRepository.save(dbPlayer);
            }
            log.debug("RoomPlayer saved to DB: id={}", playerState.id());
        }

        // Kicks flush
        List<RoomKickStateDto> kicks = roomStateStore.getKicks(roomId);
        for (RoomKickStateDto kick : kicks) {
            if (!roomKickRepository.existsByRoomIdAndUserId(kick.roomId(), kick.userId())) {
                RoomKick roomKick = new RoomKick(kick.roomId(), kick.userId(), kick.kickedByUserId());
                roomKickRepository.save(roomKick);
                log.debug("RoomKick saved to DB: roomId={}, userId={}", kick.roomId(), kick.userId());
            }
        }

        // HostHistory flush
        List<RoomHostHistoryStateDto> histories = roomStateStore.getHostHistory(roomId);
        for (RoomHostHistoryStateDto history : histories) {
            RoomHostHistory roomHostHistory = new RoomHostHistory(
                    history.roomId(),
                    history.fromUserId(),
                    history.toUserId(),
                    HostChangeReason.valueOf(history.reason())
            );
            roomHostHistoryRepository.save(roomHostHistory);
            log.debug("RoomHostHistory saved to DB: roomId={}, toUserId={}", history.roomId(), history.toUserId());
        }

        log.info("Room snapshot persisted successfully: roomId={}", roomId);
    }

    @Override
    @Transactional
    public void flushRoom(UUID roomId) {
        persistRoom(roomId);
        roomStateStore.deleteRoom(roomId);
        log.info("Room snapshot flushed and Redis cleared: roomId={}", roomId);
    }

    @Override
    public void flushGame(UUID gameId) {
        log.debug("Game snapshot flush not implemented in RoomSnapshotWriter: gameId={}", gameId);
    }
}
