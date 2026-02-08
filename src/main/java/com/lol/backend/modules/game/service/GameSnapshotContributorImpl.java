package com.lol.backend.modules.game.service;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GamePlayerState;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.MatchResult;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.snapshot.GameSnapshotContributor;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.store.RankingStateStore;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSnapshotContributorImpl implements GameSnapshotContributor {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final GameStateStore gameStateStore;
    private final RankingStateStore rankingStateStore;

    @Override
    @Transactional
    public void persistGameSnapshot(GameStateDto gameState) {
        log.debug("Persisting game snapshot: gameId={}", gameState.id());

        // DB에서 Game 조회
        Game dbGame = gameRepository.findById(gameState.id()).orElse(null);
        if (dbGame == null) {
            log.warn("Game not found in DB: gameId={}", gameState.id());
            return;
        }

        // Game 필드 동기화
        dbGame.setStage(GameStage.valueOf(gameState.stage()));
        dbGame.setStageStartedAt(gameState.stageStartedAt());
        dbGame.setStageDeadlineAt(gameState.stageDeadlineAt());
        if (gameState.finishedAt() != null) {
            dbGame.setFinishedAt(gameState.finishedAt());
        }
        if (gameState.finalAlgorithmId() != null) {
            dbGame.setFinalAlgorithmId(gameState.finalAlgorithmId());
        }
        gameRepository.save(dbGame);
        log.debug("Game saved to DB: gameId={}", gameState.id());

        // Redis에서 GamePlayer 목록 조회
        List<GamePlayerStateDto> playerStates = gameStateStore.getGamePlayers(gameState.id());
        for (GamePlayerStateDto playerState : playerStates) {
            GamePlayer dbPlayer = gamePlayerRepository.findById(playerState.id()).orElse(null);
            if (dbPlayer == null) {
                log.warn("GamePlayer not found in DB: id={}", playerState.id());
                continue;
            }

            // GamePlayer 필드 동기화
            dbPlayer.setState(GamePlayerState.valueOf(playerState.state()));
            if (playerState.scoreAfter() != null) {
                dbPlayer.setScoreAfter(playerState.scoreAfter());
            }
            if (playerState.scoreDelta() != null) {
                dbPlayer.setScoreDelta(playerState.scoreDelta());
            }
            if (playerState.finalScoreValue() != null) {
                dbPlayer.setFinalScoreValue(playerState.finalScoreValue());
            }
            if (playerState.rankInGame() != null) {
                dbPlayer.setRankInGame(playerState.rankInGame());
            }
            if (playerState.solved() != null) {
                dbPlayer.setSolved(playerState.solved());
            }
            if (playerState.result() != null) {
                dbPlayer.setResult(MatchResult.valueOf(playerState.result()));
            }
            if (playerState.coinDelta() != null) {
                dbPlayer.setCoinDelta(playerState.coinDelta());
            }
            if (playerState.expDelta() != null) {
                dbPlayer.setExpDelta(playerState.expDelta());
            }
            if (playerState.leftAt() != null) {
                dbPlayer.setLeftAt(playerState.leftAt());
            }
            if (playerState.disconnectedAt() != null) {
                dbPlayer.setDisconnectedAt(playerState.disconnectedAt());
            }
            gamePlayerRepository.save(dbPlayer);
            log.debug("GamePlayer saved to DB: id={}", playerState.id());

            // 게임 종료 시 USER.active_game_id 해제 및 정산
            if (GameStage.valueOf(gameState.stage()) == GameStage.FINISHED) {
                User user = userRepository.findById(playerState.userId()).orElse(null);
                if (user == null) {
                    log.warn("User not found: userId={}", playerState.userId());
                    continue;
                }

                // active_game_id 해제
                user.setActiveGameId(null);

                // score/coin/exp 정산
                if (playerState.scoreAfter() != null) {
                    user.setScore(playerState.scoreAfter());
                    // Redis Sorted Set 랭킹 갱신
                    rankingStateStore.updateScore(user.getId(), playerState.scoreAfter());
                    log.debug("Updated ranking score in Redis: userId={}, scoreAfter={}", user.getId(), playerState.scoreAfter());
                }
                if (playerState.coinDelta() != null) {
                    user.setCoin(user.getCoin() + playerState.coinDelta());
                }
                if (playerState.expDelta() != null) {
                    user.setExp(user.getExp() + playerState.expDelta());
                }

                userRepository.save(user);
                log.debug("User updated after game finish: userId={}, scoreAfter={}, coinDelta={}, expDelta={}",
                        playerState.userId(), playerState.scoreAfter(), playerState.coinDelta(), playerState.expDelta());
            }
        }

        log.debug("Game snapshot persisted successfully: gameId={}", gameState.id());
    }
}
