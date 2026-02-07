package com.lol.backend.modules.user.service;

import com.lol.backend.common.dto.PageInfo;
import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.MatchResult;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.user.dto.*;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

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

    public UserProfileResponse getMyProfile(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));
        return UserProfileResponse.from(user);
    }

    public ActiveGameResponse getMyActiveGame(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));

        if (user.getActiveGameId() == null) {
            return null;
        }

        Game game = gameRepository.findById(user.getActiveGameId())
                .orElse(null);

        if (game == null) {
            return null;
        }

        return ActiveGameResponse.from(game);
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

        List<MatchSummaryResponse> matches = new ArrayList<>();
        for (GamePlayer gp : items) {
            Game game = gameRepository.findById(gp.getGameId()).orElse(null);
            if (game == null) {
                continue;
            }

            matches.add(new MatchSummaryResponse(
                    gp.getGameId().toString(),
                    game.getGameType().name(),
                    gp.getResult().name(),
                    gp.getScoreDelta() != null ? gp.getScoreDelta() : 0,
                    gp.getJoinedAt().toString()
            ));
        }

        return new PagedMatchListResponse(matches, PageInfo.of(limit, nextCursor));
    }
}
