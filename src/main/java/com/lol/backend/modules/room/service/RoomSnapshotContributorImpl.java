package com.lol.backend.modules.room.service;

import com.lol.backend.modules.room.entity.HostChangeReason;
import com.lol.backend.modules.room.entity.PlayerState;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomHostHistory;
import com.lol.backend.modules.room.entity.RoomKick;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.room.repo.RoomHostHistoryRepository;
import com.lol.backend.modules.room.repo.RoomKickRepository;
import com.lol.backend.modules.room.repo.RoomPlayerRepository;
import com.lol.backend.modules.room.repo.RoomRepository;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.user.entity.Language;
import com.lol.backend.state.snapshot.RoomSnapshotContributor;
import com.lol.backend.state.store.RoomStateStore;
import com.lol.backend.state.dto.RoomHostHistoryStateDto;
import com.lol.backend.state.dto.RoomKickStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSnapshotContributorImpl implements RoomSnapshotContributor {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final RoomKickRepository roomKickRepository;
    private final RoomHostHistoryRepository roomHostHistoryRepository;
    private final RoomStateStore roomStateStore;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void persistRoomSnapshot(RoomStateDto roomState) {
        log.debug("Persisting room snapshot: roomId={}", roomState.id());

        // Room upsert
        Room dbRoom = roomRepository.findById(roomState.id()).orElse(null);
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
            entityManager.persist(dbRoom);
        }
        log.debug("Room saved to DB: roomId={}", roomState.id());

        // RoomPlayer upsert
        List<RoomPlayerStateDto> playerStates = roomStateStore.getPlayers(roomState.id());
        for (RoomPlayerStateDto playerState : playerStates) {
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
                entityManager.persist(dbPlayer);
            }
            log.debug("RoomPlayer saved to DB: id={}", playerState.id());
        }

        // Kicks flush
        List<RoomKickStateDto> kicks = roomStateStore.getKicks(roomState.id());
        for (RoomKickStateDto kick : kicks) {
            if (!roomKickRepository.existsByRoomIdAndUserId(kick.roomId(), kick.userId())) {
                RoomKick roomKick = new RoomKick(kick.roomId(), kick.userId(), kick.kickedByUserId());
                roomKickRepository.save(roomKick);
                log.debug("RoomKick saved to DB: roomId={}, userId={}", kick.roomId(), kick.userId());
            }
        }

        // HostHistory flush
        List<RoomHostHistoryStateDto> histories = roomStateStore.getHostHistory(roomState.id());
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

        log.debug("Room snapshot persisted successfully: roomId={}", roomState.id());
    }
}
