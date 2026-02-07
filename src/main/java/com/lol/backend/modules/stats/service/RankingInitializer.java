package com.lol.backend.modules.stats.service;

import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.RankingStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 서버 시작 시 DB의 모든 사용자 점수를 Redis Sorted Set에 로드한다.
 */
@Component
@Profile("!test")
public class RankingInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RankingInitializer.class);

    private final UserRepository userRepository;
    private final RankingStateStore rankingStateStore;

    public RankingInitializer(UserRepository userRepository, RankingStateStore rankingStateStore) {
        this.userRepository = userRepository;
        this.rankingStateStore = rankingStateStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing player rankings in Redis...");
        List<User> allUsers = userRepository.findAll();
        List<RankingStateStore.UserScore> userScores = allUsers.stream()
                .map(user -> new RankingStateStore.UserScore(user.getId(), user.getScore()))
                .collect(Collectors.toList());
        rankingStateStore.initializeRankings(userScores);
        log.info("Player rankings initialized: {} users loaded", userScores.size());
    }
}
