package com.lol.backend.state.impl;

import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import com.lol.backend.state.snapshot.BanPickSnapshotContributor;
import com.lol.backend.state.snapshot.GameSnapshotContributor;
import com.lol.backend.state.snapshot.RoomSnapshotContributor;
import com.lol.backend.state.snapshot.SnapshotWriter;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.store.RoomStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Primary
public class SnapshotWriterImpl implements SnapshotWriter {

    private final RoomStateStore roomStateStore;
    private final GameStateStore gameStateStore;
    private final RoomSnapshotContributor roomSnapshotContributor;
    private final GameSnapshotContributor gameSnapshotContributor;
    private final BanPickSnapshotContributor banPickSnapshotContributor;

    public SnapshotWriterImpl(
            RoomStateStore roomStateStore,
            GameStateStore gameStateStore,
            RoomSnapshotContributor roomSnapshotContributor,
            GameSnapshotContributor gameSnapshotContributor,
            BanPickSnapshotContributor banPickSnapshotContributor
    ) {
        this.roomStateStore = roomStateStore;
        this.gameStateStore = gameStateStore;
        this.roomSnapshotContributor = roomSnapshotContributor;
        this.gameSnapshotContributor = gameSnapshotContributor;
        this.banPickSnapshotContributor = banPickSnapshotContributor;
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

        roomSnapshotContributor.persistRoomSnapshot(roomState);

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
    @Transactional
    public void flushGame(UUID gameId) {
        log.info("Flushing game snapshot to DB: gameId={}", gameId);

        // Redis에서 Game 상태 조회
        GameStateDto gameState = gameStateStore.getGame(gameId).orElse(null);
        if (gameState == null) {
            log.warn("Game state not found in Redis: gameId={}", gameId);
            return;
        }

        // Game/GamePlayer 스냅샷 저장 (User 정산 포함)
        gameSnapshotContributor.persistGameSnapshot(gameState);

        // Ban/Pick 스냅샷 저장
        banPickSnapshotContributor.persistBanPickSnapshot(gameId);

        // Redis에서 게임 데이터 삭제 (종료된 게임만)
        if (GameStage.valueOf(gameState.stage()) == GameStage.FINISHED) {
            gameStateStore.deleteGame(gameId);
            log.debug("Game state deleted from Redis: gameId={}", gameId);
        }

        log.info("Game snapshot flushed successfully: gameId={}", gameId);
    }
}
