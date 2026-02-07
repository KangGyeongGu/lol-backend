package com.lol.backend.modules.room.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.room.dto.*;
import com.lol.backend.modules.room.entity.*;
import com.lol.backend.modules.room.event.RoomEventPublisher;
import com.lol.backend.modules.room.repo.*;
import com.lol.backend.modules.user.entity.Language;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.GameStateStore;
import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.SnapshotWriter;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
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

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final RoomKickRepository roomKickRepository;
    private final RoomHostHistoryRepository roomHostHistoryRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;
    private final RoomStateStore roomStateStore;
    private final GameStateStore gameStateStore;
    private final SnapshotWriter snapshotWriter;

    // ========== 1. getRooms ==========
    @Transactional(readOnly = true)
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

        // 페이징 처리 (cursor 기반, 간단 구현)
        Instant cursorUpdatedAt = CursorUtils.decode(cursor);
        List<RoomStateDto> paginated = filteredRooms.stream()
                .filter(room -> cursorUpdatedAt == null || room.updatedAt().isBefore(cursorUpdatedAt))
                .limit(limit + 1)
                .collect(Collectors.toList());

        boolean hasNext = paginated.size() > limit;
        if (hasNext) {
            paginated = paginated.subList(0, limit);
        }

        // RoomSummaryResponse 변환
        List<RoomSummaryResponse> items = paginated.stream()
                .map(roomState -> {
                    int currentPlayers = roomStateStore.getPlayers(roomState.id())
                            .stream()
                            .filter(p -> p.leftAt() == null)
                            .toList()
                            .size();

                    // 플레이어가 0인 방은 제외
                    if (currentPlayers == 0) return null;

                    boolean hasActiveGame = gameRepository.findByRoomId(roomState.id()).isPresent();
                    boolean isKicked = roomKickRepository.existsByRoomIdAndUserId(roomState.id(), currentUserId);

                    String status = hasActiveGame ? "IN_GAME" : "WAITING";
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
                .collect(Collectors.toList());

        String nextCursor = hasNext && !paginated.isEmpty()
                ? CursorUtils.encode(paginated.get(paginated.size() - 1).updatedAt())
                : null;

        return new PagedRoomListResponse(
                items,
                com.lol.backend.common.dto.PageInfo.of(limit, nextCursor),
                roomStateStore.getListVersion()
        );
    }

    // ========== 2. createRoom ==========
    @Transactional
    public RoomDetailResponse createRoom(UUID userId, CreateRoomRequest request) {
        User user = findUserOrThrow(userId);

        // active game guard
        if (user.getActiveGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        Room room = new Room(
                request.roomName(),
                request.gameType(),
                request.language(),
                request.maxPlayers(),
                userId
        );
        roomRepository.save(room);

        // Redis에 Room 상태 저장
        RoomStateDto roomState = new RoomStateDto(
                room.getId(),
                room.getRoomName(),
                room.getGameType().name(),
                room.getLanguage().name(),
                room.getMaxPlayers(),
                room.getHostUserId(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
        roomStateStore.saveRoom(roomState);

        // Creator joins as host (READY by default for host)
        RoomPlayer roomPlayer = new RoomPlayer(room.getId(), user, PlayerState.READY);
        roomPlayerRepository.save(roomPlayer);

        // Redis에 RoomPlayer 상태 저장
        RoomPlayerStateDto playerState = new RoomPlayerStateDto(
                roomPlayer.getId(),
                roomPlayer.getRoomId(),
                roomPlayer.getUserId(),
                roomPlayer.getState().name(),
                roomPlayer.getJoinedAt(),
                null,
                null
        );
        roomStateStore.addPlayer(playerState);

        // Host history (write-through, 이력 기록)
        roomHostHistoryRepository.save(
                new RoomHostHistory(room.getId(), null, userId, HostChangeReason.SYSTEM)
        );

        roomStateStore.incrementListVersion();
        eventPublisher.roomCreated(room.getId());

        return buildRoomDetailResponse(room.getId());
    }

    // ========== 3. getRoomDetail ==========
    @Transactional(readOnly = true)
    public RoomDetailResponse getRoomDetail(UUID roomId) {
        return buildRoomDetailResponse(roomId);
    }

    // ========== 4. joinRoom ==========
    @Transactional
    public RoomDetailResponse joinRoom(UUID roomId, UUID userId) {
        User user = findUserOrThrow(userId);

        // active game guard
        if (user.getActiveGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // kick guard
        if (roomKickRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.KICKED_USER);
        }

        // already in room check
        List<RoomPlayerStateDto> activePlayers = getActivePlayers(roomId);
        if (activePlayers.stream().anyMatch(p -> p.userId().equals(userId))) {
            return buildRoomDetailResponse(roomId);
        }

        // full guard
        if (activePlayers.size() >= roomState.maxPlayers()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // in-game guard
        if (gameRepository.findByRoomId(roomId).isPresent()) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        RoomPlayer roomPlayer = new RoomPlayer(roomId, user, PlayerState.UNREADY);
        roomPlayerRepository.save(roomPlayer);

        // Redis에 RoomPlayer 상태 저장
        RoomPlayerStateDto playerState = new RoomPlayerStateDto(
                roomPlayer.getId(),
                roomPlayer.getRoomId(),
                roomPlayer.getUserId(),
                roomPlayer.getState().name(),
                roomPlayer.getJoinedAt(),
                null,
                null
        );
        roomStateStore.addPlayer(playerState);

        roomStateStore.incrementListVersion();
        eventPublisher.playerJoined(roomId, userId);

        return buildRoomDetailResponse(roomId);
    }

    // ========== 5. leaveRoom ==========
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomPlayerStateDto playerState = roomStateStore.getPlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Redis에서 플레이어 leftAt 갱신 (write-back: Redis 단일 진실)
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
            // 게임이 시작되지 않은 방만 물리 삭제
            if (gameRepository.findByRoomId(roomId).isEmpty()) {
                roomHostHistoryRepository.deleteAllByRoomId(roomId);
                roomKickRepository.deleteAllByRoomId(roomId);
                roomPlayerRepository.deleteAllByRoomId(roomId);
                roomRepository.deleteById(roomId);
                roomStateStore.deleteRoom(roomId);
            } else {
                // 게임이 시작된 경우 스냅샷 반영
                snapshotWriter.flushRoom(roomId);
            }
            roomStateStore.incrementListVersion();
            eventPublisher.roomRemoved(roomId);
            return;
        }

        // Host delegation if the leaving player was host
        if (roomState.hostUserId().equals(userId)) {
            RoomPlayerStateDto newHostPlayer = remaining.get(0);
            UUID newHostUserId = newHostPlayer.userId();

            // Redis에 Room hostUserId 갱신 (write-back: Redis 단일 진실)
            RoomStateDto updatedRoom = new RoomStateDto(
                    roomState.id(),
                    roomState.roomName(),
                    roomState.gameType(),
                    roomState.language(),
                    roomState.maxPlayers(),
                    newHostUserId,
                    roomState.createdAt(),
                    Instant.now()
            );
            roomStateStore.saveRoom(updatedRoom);

            // Redis에 새 방장 상태 READY로 갱신 (write-back: Redis 단일 진실)
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

            // Host history (write-through, 이력 기록)
            roomHostHistoryRepository.save(
                    new RoomHostHistory(roomId, userId, newHostUserId, HostChangeReason.LEAVE)
            );

            eventPublisher.hostChanged(roomId, newHostUserId);
        }

        roomStateStore.incrementListVersion();
        eventPublisher.playerLeft(roomId, userId);
    }

    // ========== 6. ready ==========
    @Transactional
    public RoomDetailResponse ready(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        RoomPlayerStateDto playerState = roomStateStore.getPlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Redis 상태 갱신 (write-back: Redis 단일 진실)
        roomStateStore.updatePlayerState(roomId, userId, PlayerState.READY.name());

        eventPublisher.playerStateChanged(roomId, userId, PlayerState.READY.name());

        return buildRoomDetailResponse(roomId);
    }

    // ========== 7. unready ==========
    @Transactional
    public RoomDetailResponse unready(UUID roomId, UUID userId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        RoomPlayerStateDto playerState = roomStateStore.getPlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Redis 상태 갱신 (write-back: Redis 단일 진실)
        roomStateStore.updatePlayerState(roomId, userId, PlayerState.UNREADY.name());

        eventPublisher.playerStateChanged(roomId, userId, PlayerState.UNREADY.name());

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

        if (gameRepository.findByRoomId(roomId).isPresent()) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        List<RoomPlayerStateDto> activePlayers = getActivePlayers(roomId);

        boolean allReady = activePlayers.stream()
                .filter(rp -> !rp.userId().equals(userId))
                .allMatch(rp -> rp.state().equals(PlayerState.READY.name()));

        if (!allReady) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

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
            GamePlayer gp = new GamePlayer(game.getId(), player.getId(), player.getScore());
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
                    gp.getCoinDelta(),
                    gp.getExpDelta(),
                    gp.getJoinedAt(),
                    gp.getLeftAt(),
                    gp.getDisconnectedAt()
            );
            gameStateStore.saveGamePlayer(gpState);

            player.setActiveGameId(game.getId());
            userRepository.save(player);
        }

        roomStateStore.incrementListVersion();
        eventPublisher.gameStarted(roomId, game.getId());

        return ActiveGameResponse.from(game);
    }

    // ========== 9. kickPlayer ==========
    @Transactional
    public RoomDetailResponse kickPlayer(UUID roomId, UUID userId, UUID targetUserId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!roomState.hostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        RoomPlayerStateDto targetPlayer = roomStateStore.getPlayer(roomId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // Kick 기록 (write-through, 이력 기록)
        RoomKick kick = new RoomKick(roomId, targetUserId, userId);
        roomKickRepository.save(kick);

        // Redis에서 플레이어 leftAt 갱신 (write-back: Redis 단일 진실)
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
        eventPublisher.playerKicked(roomId, targetUserId);

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

    private RoomDetailResponse buildRoomDetailResponse(UUID roomId) {
        RoomStateDto roomState = roomStateStore.getRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        List<RoomPlayerStateDto> players = getActivePlayers(roomId);

        // RoomPlayerResponse 변환
        List<RoomPlayerResponse> playerResponses = players.stream()
                .map(p -> {
                    User user = findUserOrThrow(p.userId());
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
