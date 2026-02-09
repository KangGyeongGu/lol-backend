package com.lol.backend.modules.stats.service;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.catalog.entity.Algorithm;
import com.lol.backend.modules.catalog.repo.AlgorithmRepository;
import com.lol.backend.modules.game.repo.GameBanRepository;
import com.lol.backend.modules.game.repo.GamePickRepository;
import com.lol.backend.modules.stats.dto.AlgorithmPickBanRateResponse;
import com.lol.backend.modules.stats.dto.ListOfAlgorithmPickBanRatesResponse;
import com.lol.backend.modules.stats.dto.ListOfPlayerRankingsResponse;
import com.lol.backend.modules.stats.dto.PlayerRankingResponse;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.store.RankingStateStore;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 통계/랭킹 도메인 서비스.
 * 실시간 플레이어 랭킹과 알고리즘 밴/픽률 통계를 제공한다.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final AlgorithmRepository algorithmRepository;
    private final GameBanRepository gameBanRepository;
    private final GamePickRepository gamePickRepository;
    private final RankingStateStore rankingStateStore;

    /**
     * 실시간 플레이어 랭킹 조회.
     * Redis Sorted Set에서 상위 100명의 플레이어를 점수 기준 내림차순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public ListOfPlayerRankingsResponse getPlayerRankings() {
        // Redis에서 상위 100명의 userId 조회
        List<UUID> topUserIds = rankingStateStore.getTopPlayers(100);
        if (topUserIds.isEmpty()) {
            return ListOfPlayerRankingsResponse.of(List.of());
        }

        // DB에서 사용자 상세 정보 조회 (bulk query)
        List<User> topUsers = userRepository.findAllById(topUserIds);
        Map<UUID, User> userMap = topUsers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Redis 순서대로 랭킹 리스트 생성
        List<PlayerRankingResponse> rankings = new ArrayList<>();
        for (int i = 0; i < topUserIds.size(); i++) {
            UUID userId = topUserIds.get(i);
            User user = userMap.get(userId);
            if (user != null) {
                rankings.add(PlayerRankingResponse.of(
                        i + 1,
                        user.getId().toString(),
                        user.getNickname(),
                        user.getScore(),
                        user.getTier()
                ));
            }
        }

        return ListOfPlayerRankingsResponse.of(rankings);
    }

    /**
     * 실시간 알고리즘 밴/픽률 조회.
     *
     * 현재 구현: 빈 목록 반환 (향후 game/ban/pick 테이블 연동 필요).
     * 실제 산출 규칙:
     * - 최근 N일간 RANKED 게임의 밴/픽 데이터 집계
     * - pickRate = (해당 알고리즘 픽 횟수) / (전체 RANKED 게임 수)
     * - banRate = (해당 알고리즘 밴 횟수) / (전체 RANKED 게임 수)
     */
    @Transactional(readOnly = true)
    public ListOfAlgorithmPickBanRatesResponse getAlgorithmPickBanRates() {
        List<Game> rankedGames = gameRepository.findByGameType(GameType.RANKED);
        if (rankedGames.isEmpty()) {
            return ListOfAlgorithmPickBanRatesResponse.of(List.of());
        }

        List<UUID> gameIds = rankedGames.stream().map(Game::getId).toList();
        long totalGames = rankedGames.size();

        Map<UUID, Long> banCounts = gameBanRepository.countByAlgorithmIdGrouped(gameIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        Map<UUID, Long> pickCounts = gamePickRepository.countByAlgorithmIdGrouped(gameIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        Set<UUID> allAlgorithmIds = new HashSet<>();
        allAlgorithmIds.addAll(banCounts.keySet());
        allAlgorithmIds.addAll(pickCounts.keySet());

        Map<UUID, Algorithm> algorithmMap = algorithmRepository.findAllById(allAlgorithmIds).stream()
                .collect(Collectors.toMap(Algorithm::getId, a -> a));

        List<AlgorithmPickBanRateResponse> items = allAlgorithmIds.stream()
                .filter(algorithmMap::containsKey)
                .map(id -> {
                    Algorithm algo = algorithmMap.get(id);
                    double pickRate = pickCounts.getOrDefault(id, 0L) / (double) totalGames;
                    double banRate = banCounts.getOrDefault(id, 0L) / (double) totalGames;
                    return AlgorithmPickBanRateResponse.of(
                            id.toString(), algo.getName(), pickRate, banRate
                    );
                })
                .toList();

        return ListOfAlgorithmPickBanRatesResponse.of(items);
    }
}
