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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final RoomKickRepository roomKickRepository;
    private final RoomHostHistoryRepository roomHostHistoryRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;

    public RoomService(RoomRepository roomRepository,
                       RoomPlayerRepository roomPlayerRepository,
                       RoomKickRepository roomKickRepository,
                       RoomHostHistoryRepository roomHostHistoryRepository,
                       GameRepository gameRepository,
                       GamePlayerRepository gamePlayerRepository,
                       UserRepository userRepository,
                       RoomEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.roomKickRepository = roomKickRepository;
        this.roomHostHistoryRepository = roomHostHistoryRepository;
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    // ========== 1. getRooms ==========
    @Transactional(readOnly = true)
    public PagedRoomListResponse getRooms(UUID currentUserId,
                                          String roomName,
                                          Language language,
                                          GameType gameType,
                                          String cursor,
                                          int limit) {
        Instant cursorUpdatedAt = CursorUtils.decode(cursor);

        List<Room> rooms = roomRepository.findRoomsWithFilters(
                roomName, language, gameType, cursorUpdatedAt, limit + 1
        );

        boolean hasNext = rooms.size() > limit;
        if (hasNext) {
            rooms = rooms.subList(0, limit);
        }

        List<RoomSummaryResponse> items = rooms.stream()
                .filter(room -> {
                    // 활성 플레이어가 0인 방 제외
                    if (roomPlayerRepository.countActivePlayersByRoomId(room.getId()) == 0) return false;
                    // 종료된 게임이 연결된 방 제외
                    return gameRepository.findByRoomId(room.getId())
                            .map(game -> !game.isFinished())
                            .orElse(true);
                })
                .map(room -> {
                    int currentPlayers = roomPlayerRepository.countActivePlayersByRoomId(room.getId());
                    boolean hasActiveGame = gameRepository.findByRoomId(room.getId()).isPresent();
                    boolean isKicked = roomKickRepository.existsByRoomIdAndUserId(room.getId(), currentUserId);
                    return RoomSummaryResponse.from(room, currentPlayers, hasActiveGame, isKicked);
                })
                .toList();

        String nextCursor = hasNext && !rooms.isEmpty()
                ? CursorUtils.encode(rooms.get(rooms.size() - 1).getUpdatedAt())
                : null;

        return new PagedRoomListResponse(
                items,
                com.lol.backend.common.dto.PageInfo.of(limit, nextCursor),
                eventPublisher.getListVersion()
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

        // Creator joins as host (READY by default for host)
        RoomPlayer roomPlayer = new RoomPlayer(room.getId(), user, PlayerState.READY);
        roomPlayerRepository.save(roomPlayer);

        // Host history
        roomHostHistoryRepository.save(
                new RoomHostHistory(room.getId(), null, userId, HostChangeReason.SYSTEM)
        );

        eventPublisher.roomCreated(room.getId());

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(room.getId());
        return RoomDetailResponse.from(room, activePlayers);
    }

    // ========== 3. getRoomDetail ==========
    @Transactional(readOnly = true)
    public RoomDetailResponse getRoomDetail(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);
        return RoomDetailResponse.from(room, activePlayers);
    }

    // ========== 4. joinRoom ==========
    @Transactional
    public RoomDetailResponse joinRoom(UUID roomId, UUID userId) {
        User user = findUserOrThrow(userId);

        // active game guard
        if (user.getActiveGameId() != null) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // kick guard
        if (roomKickRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.KICKED_USER);
        }

        // already in room check
        if (roomPlayerRepository.findActivePlayer(roomId, userId).isPresent()) {
            List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);
            return RoomDetailResponse.from(room, activePlayers);
        }

        // full guard
        int currentPlayers = roomPlayerRepository.countActivePlayersByRoomId(roomId);
        if (currentPlayers >= room.getMaxPlayers()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // in-game guard
        if (gameRepository.findByRoomId(roomId).isPresent()) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        RoomPlayer roomPlayer = new RoomPlayer(roomId, user, PlayerState.UNREADY);
        roomPlayerRepository.save(roomPlayer);

        eventPublisher.playerJoined(roomId, userId);

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);
        return RoomDetailResponse.from(room, activePlayers);
    }

    // ========== 5. leaveRoom ==========
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        RoomPlayer roomPlayer = roomPlayerRepository.findActivePlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        roomPlayer.leave();
        roomPlayerRepository.save(roomPlayer);

        List<RoomPlayer> remaining = roomPlayerRepository.findActivePlayersByRoomId(roomId);

        if (remaining.isEmpty()) {
            // 게임이 시작되지 않은 방만 물리 삭제
            if (gameRepository.findByRoomId(roomId).isEmpty()) {
                roomHostHistoryRepository.deleteAllByRoomId(roomId);
                roomKickRepository.deleteAllByRoomId(roomId);
                roomPlayerRepository.deleteAllByRoomId(roomId);
                roomRepository.delete(room);
            }
            eventPublisher.roomRemoved(roomId);
            return;
        }

        // Host delegation if the leaving player was host
        if (room.getHostUserId().equals(userId)) {
            RoomPlayer newHost = remaining.get(0);
            UUID newHostUserId = newHost.getUserId();

            room.setHostUserId(newHostUserId);
            newHost.setState(PlayerState.READY);
            roomPlayerRepository.save(newHost);

            roomHostHistoryRepository.save(
                    new RoomHostHistory(roomId, userId, newHostUserId, HostChangeReason.LEAVE)
            );

            eventPublisher.hostChanged(roomId, newHostUserId);
        }

        roomRepository.save(room);
        eventPublisher.playerLeft(roomId, userId);
    }

    // ========== 6. ready ==========
    @Transactional
    public RoomDetailResponse ready(UUID roomId, UUID userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getHostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        RoomPlayer roomPlayer = roomPlayerRepository.findActivePlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        roomPlayer.setState(PlayerState.READY);
        roomPlayerRepository.save(roomPlayer);

        eventPublisher.playerStateChanged(roomId, userId, PlayerState.READY.name());

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);
        return RoomDetailResponse.from(room, activePlayers);
    }

    // ========== 7. unready ==========
    @Transactional
    public RoomDetailResponse unready(UUID roomId, UUID userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getHostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        RoomPlayer roomPlayer = roomPlayerRepository.findActivePlayer(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        roomPlayer.setState(PlayerState.UNREADY);
        roomPlayerRepository.save(roomPlayer);

        eventPublisher.playerStateChanged(roomId, userId, PlayerState.UNREADY.name());

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);
        return RoomDetailResponse.from(room, activePlayers);
    }

    // ========== 8. startGame ==========
    @Transactional
    public ActiveGameResponse startGame(UUID roomId, UUID userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        if (gameRepository.findByRoomId(roomId).isPresent()) {
            throw new BusinessException(ErrorCode.ACTIVE_GAME_EXISTS);
        }

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);

        boolean allReady = activePlayers.stream()
                .filter(rp -> !rp.getUserId().equals(userId))
                .allMatch(rp -> rp.getState() == PlayerState.READY);

        if (!allReady) {
            throw new BusinessException(ErrorCode.INVALID_PLAYER_STATE);
        }

        Game game = new Game(roomId, room.getGameType());
        gameRepository.save(game);

        for (RoomPlayer rp : activePlayers) {
            User player = findUserOrThrow(rp.getUserId());
            GamePlayer gp = new GamePlayer(game.getId(), player.getId(), player.getScore());
            gamePlayerRepository.save(gp);

            player.setActiveGameId(game.getId());
            userRepository.save(player);
        }

        roomRepository.save(room);
        eventPublisher.gameStarted(roomId, game.getId());

        return ActiveGameResponse.from(game);
    }

    // ========== 9. kickPlayer ==========
    @Transactional
    public RoomDetailResponse kickPlayer(UUID roomId, UUID userId, UUID targetUserId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHostUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_HOST);
        }

        RoomPlayer targetPlayer = roomPlayerRepository.findActivePlayer(roomId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        RoomKick kick = new RoomKick(roomId, targetUserId, userId);
        roomKickRepository.save(kick);

        targetPlayer.leave();
        roomPlayerRepository.save(targetPlayer);

        roomRepository.save(room);
        eventPublisher.playerKicked(roomId, targetUserId);

        List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(roomId);
        return RoomDetailResponse.from(room, activePlayers);
    }

    // ========== Helper ==========
    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
