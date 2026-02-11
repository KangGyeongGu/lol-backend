package com.lol.backend.modules.user.service;

import com.lol.backend.common.dto.PageInfo;
import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.entity.MatchResult;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.repo.RoomRepository;
import com.lol.backend.modules.user.dto.*;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.dto.GameStateDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final RoomRepository roomRepository;
    private final GameStateStore gameStateStore;

    public UserProfileResponse getMyProfile(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "사용자 정보를 찾을 수 없습니다"));
        return UserProfileResponse.from(user);
    }

    public ActiveGameResponse getMyActiveGame(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "사용자 정보를 찾을 수 없습니다"));

        if (user.getActiveGameId() == null) {
            return null;
        }

        // Redis SSOT: GameStateStore에서 진행 중인 게임 상태 조회
        GameStateDto gameState = gameStateStore.getGame(user.getActiveGameId())
                .orElse(null);

        if (gameState == null) {
            // Redis에 게임이 없으면 active game이 없는 것으로 처리
            return null;
        }

        return ActiveGameResponse.from(gameState);
    }

    public UserStatsResponse getMyStats(String userId) {
        UUID userUuid = UUID.fromString(userId);
        List<GamePlayer> finishedGames = gamePlayerRepository.findByUserIdAndResultIsNotNull(userUuid);

        int games = finishedGames.size();
        int wins = 0;
        int losses = 0;
        int draws = 0;

        for (GamePlayer gp : finishedGames) {
            MatchResult result = gp.getResult();
            if (result == MatchResult.WIN) {
                wins++;
            } else if (result == MatchResult.LOSE) {
                losses++;
            } else if (result == MatchResult.DRAW) {
                draws++;
            }
        }

        double winRate = games == 0 ? 0.0 : (double) wins / games;

        return new UserStatsResponse(games, wins, losses, draws, winRate);
    }

    public PagedMatchListResponse getMyMatches(String userId, String cursor, int limit) {
        UUID userUuid = UUID.fromString(userId);
        Pageable pageable = Pageable.ofSize(limit + 1);

        List<GamePlayer> gamePlayers;
        if (cursor == null) {
            gamePlayers = gamePlayerRepository.findByUserIdAndResultIsNotNullOrderByJoinedAtDesc(userUuid, pageable);
        } else {
            Instant before = Instant.parse(cursor);
            gamePlayers = gamePlayerRepository.findByUserIdAndResultIsNotNullAndJoinedAtBeforeOrderByJoinedAtDesc(userUuid, before, pageable);
        }

        boolean hasNext = gamePlayers.size() > limit;
        List<GamePlayer> items = hasNext ? gamePlayers.subList(0, limit) : gamePlayers;

        String nextCursor = null;
        if (hasNext) {
            nextCursor = items.get(items.size() - 1).getJoinedAt().toString();
        }

        // N+1 방지: bulk query
        List<UUID> gameIds = items.stream().map(GamePlayer::getGameId).distinct().toList();
        var gameMap = gameRepository.findAllById(gameIds).stream()
                .collect(java.util.stream.Collectors.toMap(Game::getId, g -> g));

        List<UUID> roomIds = gameMap.values().stream()
                .map(Game::getRoomId)
                .distinct()
                .toList();
        var roomMap = roomRepository.findAllById(roomIds).stream()
                .collect(java.util.stream.Collectors.toMap(Room::getId, r -> r));

        // finalPlayers bulk query: gameId별 result가 있는 플레이어 수
        List<GamePlayer> allGamePlayers = gamePlayerRepository.findByGameIdIn(gameIds);
        var finalPlayersMap = allGamePlayers.stream()
                .filter(p -> p.getResult() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        GamePlayer::getGameId,
                        java.util.stream.Collectors.counting()
                ));

        List<MatchSummaryResponse> matches = new ArrayList<>();
        for (GamePlayer gp : items) {
            Game game = gameMap.get(gp.getGameId());
            if (game == null) {
                continue;
            }

            Room room = roomMap.get(game.getRoomId());
            if (room == null) {
                continue;
            }

            int finalPlayers = finalPlayersMap.getOrDefault(gp.getGameId(), 0L).intValue();

            matches.add(new MatchSummaryResponse(
                    gp.getGameId().toString(),
                    room.getRoomName(),
                    game.getGameType().name(),
                    gp.getResult().name(),
                    finalPlayers,
                    gp.getJoinedAt().toString()
            ));
        }

        return new PagedMatchListResponse(matches, PageInfo.of(limit, nextCursor));
    }
}
