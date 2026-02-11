package com.lol.backend.modules.room.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.game.service.GameService;
import com.lol.backend.modules.room.dto.*;
import com.lol.backend.modules.room.entity.*;
import com.lol.backend.modules.room.event.RoomEventPublisher;
import com.lol.backend.modules.game.event.GameEventPublisher;
import com.lol.backend.modules.user.entity.Language;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.snapshot.SnapshotWriter;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.store.RoomStateStore;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.dto.RoomHostHistoryStateDto;
import com.lol.backend.state.dto.RoomKickStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;
    private final GameEventPublisher gameEventPublisher;
    private final RoomStateStore roomStateStore;
    private final GameStateStore gameStateStore;
    private final SnapshotWriter snapshotWriter;
    private final GameService gameService;

    // ========== 1. getRooms ==========
    public PagedRoomListResponse getRooms(UUID currentUserId,
                                          String roomName,
                                          Language language,
                                          GameType gameType,
                                          String cursor,
                                          int limit) {
        // Redis에서 활성 룸 목록 조회
        List<RoomStateDto> allRooms = roomStateStore.getAllActiveRooms();

        // 필터링 적용
        List<RoomStateDto> filteredRooms = allRooms.stream()
                .filter(room -> roomName == null || room.roomName().contains(roomName))
                .filter(room -> language == null || room.language().equals(language.name()))
                .filter(room -> gameType == null || room.gameType().equals(gameType.name()))
                .sorted((r1, r2) -> r2.updatedAt().compareTo(r1.updatedAt()))
                .collect(Collectors.toList());

        // Filter out rooms with 0 players before pagination
        Instant cursorUpdatedAt = CursorUtils.decode(cursor);
        List<RoomSummaryResponse> allItems = filteredRooms.stream()
                .filter(room -> cursorUpdatedAt == null || room.updatedAt().isBefore(cursorUpdatedAt))
                .map(roomState -> {
                    int currentPlayers = roomStateStore.getPlayers(roomState.id())
                            .stream()
                            .filter(p -> p.leftAt() == null)
                            .toList()
                            .size();

                    // Filter out rooms with 0 players
                    if (currentPlayers == 0) return null;

                    boolean hasActiveGame = roomState.activeGameId() != null;
                    boolean isKicked = roomStateStore.isKicked(roomState.id(), currentUserId);

                    RoomStatus status = hasActiveGame ? RoomStatus.IN_GAME : RoomStatus.WAITING;
                    boolean joinable = !hasActiveGame
                            && !isKicked
                            && currentPlayers < roomState.maxPlayers();

                    return new RoomSummaryResponse(
                            roomState.id().toString(),
                            roomState.roomName(),
                            GameType.valueOf(roomState.gameType()),
                            Language.valueOf(roomState.language()),
                            roomState.maxPlayers(),
                            currentPlayers,
                            status,
                            joinable,
                            roomState.updatedAt()
                    );
                })
                .filter(r -> r != null)
                .limit(limit + 1)
                .collect(Collectors.toList());

        // Pagination
        boolean hasNext = allItems.size() > limit;
        List<RoomSummaryResponse> items = hasNext ? allItems.subList(0, limit) : allItems;

        String nextCursor = hasNext && !items.isEmpty()
                ? CursorUtils.encode(items.get(items.size() - 1).updatedAt())
                : null;

        return new PagedRoomListResponse(
                items,
                com.lol.backend.common.dto.PageInfo.of(limit, nextCursor),
                roomStateStore.getListVersion()
        );
    }

    // ========== 2. createRoom ==========
    public RoomDetailResponse createRoom(UUID userId, CreateRoomRequest request) {
        User user = findUserOrThrow(userId);

        // active game guard
        if (user.getActiveGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        UUID roomId = UUID.randomUUID();
        Instant now = Instant.now();

        // Redis에 Room 상태 저장 (DB 접근 없음)
        RoomStateDto roomState = new RoomStateDto(
                roomId,
                request.roomName(),
                request.gameType().name(),
                request.language().name(),
                request.maxPlayers(),
                userId,
                null,
                now,
                now
        );
        roomStateStore.saveRoom(roomState);

        // Redis에 RoomPlayer 상태 저장 (DB 접근 없음)
        UUID playerId = UUID.randomUUID();
        RoomPlayerStateDto playerState = new RoomPlayerStateDto(
                playerId,
                roomId,
                userId,
                PlayerState.READY.name(),
                now,
                null,
                null
        );
        roomStateStore.addPlayer(playerState);

        // Host history → Redis
        roomStateStore.addHostHistory(new RoomHostHistoryStateDto(
                roomId, null, userId, HostChangeReason.SYSTEM.name(), now
        ));

        roomStateStore.incrementListVersion();
        long listVersion = roomStateStore.getListVersion();
        RoomSummaryResponse summary = buildRoomSummary(roomId);
        eventPublisher.roomListUpsert(summary, listVersion);

        return buildRoomDetailResponse(roomId);
    }

    // ========== 3. getRoomDetail ==========
    public RoomDetailResponse getRoomDetail(UUID roomId) {
        return buildRoomDetailResponse(roomId);
    }

    // ========== 4. joinRoom ==========
    public RoomDetailResponse joinRoom(UUID roomId, UUID userId) {
        User user = findUserOrThrow(userId);

        // active game guard
        if (user.getActiveGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // kick guard → Redis
        if (roomStateStore.isKicked(roomId, userId)) {
            throw new BusinessException(ErrorCode.KICKED_USER);
        }

        // full guard (check before atomic add to avoid unnecessary attempts)
        List<RoomPlayerStateDto> activePlayers = getActivePlayers(roomId);
        if (activePlayers.size() >= roomState.maxPlayers()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // in-game guard → Redis
        if (roomState.activeGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        // Atomic add player (prevents duplicate joins)
        Instant now = Instant.now();
        UUID playerId = UUID.randomUUID();
        RoomPlayerStateDto playerState = new RoomPlayerStateDto(
                playerId,
                roomId,
                userId,
                PlayerState.UNREADY.name(),
                now,
                null,
                null
        );

        // Use atomic operation to prevent race condition
        boolean added = roomStateStore.addPlayerIfNotExists(playerState);
        if (!added) {
            // Player already in room, return current state
            return buildRoomDetailResponse(roomId);
        }

        roomStateStore.incrementListVersion();
        long listVersion = roomStateStore.getListVersion();
        eventPublisher.playerJoined(
                roomId,
                userId,
                user.getNickname(),
                PlayerState.UNREADY.name(),
                now.toString()
        );

        // Emit ROOM_LIST_UPSERT to synchronize room list after join
        RoomSummaryResponse summary = buildRoomSummary(roomId);
        eventPublisher.roomListUpsert(summary, listVersion);

        return buildRoomDetailResponse(roomId);
    }

    // ========== 5. leaveRoom ==========
    public void leaveRoom(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomPlayerStateDto playerState = roomStateStore.getPlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Redis에서 플레이어 leftAt 갱신
        RoomPlayerStateDto updatedPlayer = new RoomPlayerStateDto(
                playerState.id(),
                playerState.roomId(),
                playerState.userId(),
                playerState.state(),
                playerState.joinedAt(),
                Instant.now(),
                playerState.disconnectedAt()
        );
        roomStateStore.addPlayer(updatedPlayer);

        List<RoomPlayerStateDto> remaining = getActivePlayers(roomId);

        if (remaining.isEmpty()) {
            if (roomState.activeGameId() == null) {
                // 게임 없는 방 해체: Redis 삭제만 (DB에 아무것도 없음)
                roomStateStore.deleteRoom(roomId);
            } else {
                // 게임이 시작된 방 해체: DB에 스냅샷 반영 + Redis 삭제
                snapshotWriter.flushRoom(roomId);
            }
            roomStateStore.incrementListVersion();
            long listVersion = roomStateStore.getListVersion();
            eventPublisher.roomListRemoved(roomId, listVersion, "ROOM_CLOSED");
            return;
        }

        // Host delegation if the leaving player was host
        if (roomState.hostUserId().equals(userId)) {
            RoomPlayerStateDto newHostPlayer = remaining.get(0);
            UUID newHostUserId = newHostPlayer.userId();

            // Redis에 Room hostUserId 갱신
            RoomStateDto updatedRoom = new RoomStateDto(
                    roomState.id(),
                    roomState.roomName(),
                    roomState.gameType(),
                    roomState.language(),
                    roomState.maxPlayers(),
                    newHostUserId,
                    roomState.activeGameId(),
                    roomState.createdAt(),
                    Instant.now()
            );
            roomStateStore.saveRoom(updatedRoom);

            // Redis에 새 방장 상태 READY로 갱신
            RoomPlayerStateDto newHostUpdated = new RoomPlayerStateDto(
                    newHostPlayer.id(),
                    newHostPlayer.roomId(),
                    newHostPlayer.userId(),
                    PlayerState.READY.name(),
                    newHostPlayer.joinedAt(),
                    newHostPlayer.leftAt(),
                    newHostPlayer.disconnectedAt()
            );
            roomStateStore.addPlayer(newHostUpdated);

            // Host history → Redis
            roomStateStore.addHostHistory(new RoomHostHistoryStateDto(
                    roomId, userId, newHostUserId, HostChangeReason.LEAVE.name(), Instant.now()
            ));

            eventPublisher.hostChanged(
                    roomId,
                    userId,
                    newHostUserId,
                    "LEAVE",
                    Instant.now().toString()
            );
        }

        roomStateStore.incrementListVersion();
        long listVersion = roomStateStore.getListVersion();
        eventPublisher.playerLeft(roomId, userId, Instant.now().toString(), "LEAVE");

        // Emit ROOM_LIST_UPSERT to synchronize room list after leave (if room still exists)
        RoomSummaryResponse summary = buildRoomSummary(roomId);
        eventPublisher.roomListUpsert(summary, listVersion);
    }

    // ========== 6. ready ==========
    public RoomDetailResponse ready(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        RoomPlayerStateDto player = roomStateStore.getPlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Check if player has left the room
        if (player.leftAt() != null) {
            throw new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM);
        }

        roomStateStore.updatePlayerState(roomId, userId, PlayerState.READY.name());

        eventPublisher.playerStateChanged(
                roomId,
                userId,
                PlayerState.READY.name(),
                Instant.now().toString()
        );

        return buildRoomDetailResponse(roomId);
    }

    // ========== 7. unready ==========
    public RoomDetailResponse unready(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        RoomPlayerStateDto player = roomStateStore.getPlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Check if player has left the room
        if (player.leftAt() != null) {
            throw new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM);
        }

        roomStateStore.updatePlayerState(roomId, userId, PlayerState.UNREADY.name());

        eventPublisher.playerStateChanged(
                roomId,
                userId,
                PlayerState.UNREADY.name(),
                Instant.now().toString()
        );

        return buildRoomDetailResponse(roomId);
    }

    // ========== 8. startGame ==========
    @Transactional
    public ActiveGameResponse startGame(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        if (roomState.activeGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        List<RoomPlayerStateDto> activePlayers = getActivePlayers(roomId);

        boolean allReady = activePlayers.stream()
                .filter(rp -> !rp.userId().equals(userId))
                .allMatch(rp -> rp.state().equals(PlayerState.READY.name()));

        if (!allReady) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        // Phase 1: Room → DB flush (FK 만족)
        snapshotWriter.persistRoom(roomId);

        Game game = new Game(roomId, GameType.valueOf(roomState.gameType()));
        gameRepository.save(game);

        // Redis에 Game 상태 저장
        GameStateDto gameState = new GameStateDto(
                game.getId(),
                game.getRoomId(),
                game.getGameType().name(),
                game.getStage().name(),
                game.getStageStartedAt(),
                game.getStageDeadlineAt(),
                game.getStartedAt(),
                game.getFinishedAt(),
                game.getFinalAlgorithmId(),
                game.getCreatedAt()
        );
        gameStateStore.saveGame(gameState);

        for (RoomPlayerStateDto rp : activePlayers) {
            User player = findUserOrThrow(rp.userId());
            GamePlayer gp = new GamePlayer(game.getId(), player.getId(), player.getScore(), player.getCoin(), player.getExp());
            gamePlayerRepository.save(gp);

            // Redis에 GamePlayer 상태 저장
            GamePlayerStateDto gpState = new GamePlayerStateDto(
                    gp.getId(),
                    gp.getGameId(),
                    gp.getUserId(),
                    gp.getState().name(),
                    gp.getScoreBefore(),
                    gp.getScoreAfter(),
                    gp.getScoreDelta(),
                    gp.getFinalScoreValue(),
                    gp.getRankInGame(),
                    gp.getSolved(),
                    gp.getResult() != null ? gp.getResult().name() : null,
                    gp.getCoinBefore(),
                    gp.getCoinDelta(),
                    gp.getExpBefore(),
                    gp.getExpDelta(),
                    gp.getJoinedAt(),
                    gp.getLeftAt(),
                    gp.getDisconnectedAt()
            );
            gameStateStore.saveGamePlayer(gpState);

            player.setActiveGameId(game.getId());
            userRepository.save(player);
        }

        // Redis RoomStateDto 갱신: activeGameId 설정
        RoomStateDto updatedRoom = new RoomStateDto(
                roomState.id(),
                roomState.roomName(),
                roomState.gameType(),
                roomState.language(),
                roomState.maxPlayers(),
                roomState.hostUserId(),
                game.getId(),
                roomState.createdAt(),
                Instant.now()
        );
        roomStateStore.saveRoom(updatedRoom);

        // 동기 stage 전이: LOBBY → 첫 stage (SSOT: ROOM_GAME_STARTED.stage에 LOBBY 불가)
        GameStage firstStage = (GameType.valueOf(roomState.gameType()) == GameType.RANKED)
                ? GameStage.BAN : GameStage.PLAY;
        gameService.transitionStage(game.getId(), firstStage);

        // Redis에서 갱신된 상태 조회
        GameStateDto updatedGame = gameStateStore.getGame(game.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // ROOM_LIST_REMOVED 이벤트 발행
        roomStateStore.incrementListVersion();
        long listVersion = roomStateStore.getListVersion();
        eventPublisher.roomListRemoved(roomId, listVersion, "GAME_STARTED");

        // SSOT 계약: remainingMs와 meta.serverTime을 동일 Instant 기반으로 계산
        Instant serverTime = Instant.now();

        // ROOM_GAME_STARTED 이벤트 발행 → 대기실 클라이언트가 game 토픽 구독 전환
        String pageRoute = ActiveGameResponse.from(updatedGame).pageRoute();
        long remainingMs = (updatedGame.stageDeadlineAt() != null)
                ? Math.max(0, updatedGame.stageDeadlineAt().toEpochMilli() - serverTime.toEpochMilli())
                : 0L;
        eventPublisher.roomGameStarted(
                roomId,
                game.getId(),
                updatedGame.gameType(),
                updatedGame.stage(),
                pageRoute,
                updatedGame.stageStartedAt() != null ? updatedGame.stageStartedAt().toString() : null,
                updatedGame.stageDeadlineAt() != null ? updatedGame.stageDeadlineAt().toString() : null,
                remainingMs
        );

        // GAME_STAGE_CHANGED 이벤트 발행 (첫 stage 전이: LOBBY → BAN/PLAY)
        // SSOT 계약: 모든 stage 전이 시 GAME_STAGE_CHANGED 발행 필요
        gameEventPublisher.gameStageChanged(
                game.getId(),
                roomId,
                updatedGame.gameType(),
                updatedGame.stage(),
                updatedGame.stageStartedAt() != null ? updatedGame.stageStartedAt().toString() : null,
                updatedGame.stageDeadlineAt() != null ? updatedGame.stageDeadlineAt().toString() : null,
                remainingMs,
                serverTime
        );

        return ActiveGameResponse.from(updatedGame);
    }

    // ========== 9. kickPlayer ==========
    public RoomDetailResponse kickPlayer(UUID roomId, UUID userId, UUID targetUserId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        roomStateStore.getPlayer(roomId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Kick 기록 → Redis
        roomStateStore.addKick(new RoomKickStateDto(roomId, targetUserId, userId, Instant.now()));

        // Redis에서 플레이어 leftAt 갱신
        RoomPlayerStateDto targetPlayer = roomStateStore.getPlayer(roomId, targetUserId).get();
        RoomPlayerStateDto updatedPlayer = new RoomPlayerStateDto(
                targetPlayer.id(),
                targetPlayer.roomId(),
                targetPlayer.userId(),
                targetPlayer.state(),
                targetPlayer.joinedAt(),
                Instant.now(),
                targetPlayer.disconnectedAt()
        );
        roomStateStore.addPlayer(updatedPlayer);

        roomStateStore.incrementListVersion();
        long listVersion = roomStateStore.getListVersion();
        eventPublisher.playerKicked(
                roomId,
                targetUserId,
                userId,
                Instant.now().toString()
        );
        eventPublisher.playerLeft(roomId, targetUserId, Instant.now().toString(), "KICKED");

        // 방 목록 인원수 갱신
        RoomSummaryResponse summary = buildRoomSummary(roomId);
        eventPublisher.roomListUpsert(summary, listVersion);

        return buildRoomDetailResponse(roomId);
    }

    // ========== Helper ==========
    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private List<RoomPlayerStateDto> getActivePlayers(UUID roomId) {
        return roomStateStore.getPlayers(roomId).stream()
                .filter(p -> p.leftAt() == null)
                .collect(Collectors.toList());
    }

    private RoomSummaryResponse buildRoomSummary(UUID roomId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        int currentPlayers = getActivePlayers(roomId).size();
        boolean hasActiveGame = roomState.activeGameId() != null;
        RoomStatus status = hasActiveGame ? RoomStatus.IN_GAME : RoomStatus.WAITING;
        boolean joinable = !hasActiveGame && currentPlayers < roomState.maxPlayers();

        return new RoomSummaryResponse(
                roomState.id().toString(),
                roomState.roomName(),
                GameType.valueOf(roomState.gameType()),
                Language.valueOf(roomState.language()),
                roomState.maxPlayers(),
                currentPlayers,
                status,
                joinable,
                roomState.updatedAt()
        );
    }

    private RoomDetailResponse buildRoomDetailResponse(UUID roomId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        List<RoomPlayerStateDto> players = getActivePlayers(roomId);

        // Bulk load users to avoid N+1 query
        List<UUID> userIds = players.stream()
                .map(RoomPlayerStateDto::userId)
                .collect(Collectors.toList());

        java.util.Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // RoomPlayerResponse 변환
        List<RoomPlayerResponse> playerResponses = players.stream()
                .map(p -> {
                    User user = userMap.get(p.userId());
                    if (user == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED);
                    }
                    com.lol.backend.modules.user.dto.UserSummaryResponse userSummary =
                            com.lol.backend.modules.user.dto.UserSummaryResponse.from(user);
                    boolean isHost = p.userId().equals(roomState.hostUserId());
                    return new RoomPlayerResponse(
                            userSummary,
                            PlayerState.valueOf(p.state()),
                            isHost
                    );
                })
                .collect(Collectors.toList());

        return new RoomDetailResponse(
                roomState.id().toString(),
                roomState.roomName(),
                GameType.valueOf(roomState.gameType()),
                Language.valueOf(roomState.language()),
                roomState.maxPlayers(),
                playerResponses
        );
    }
}
