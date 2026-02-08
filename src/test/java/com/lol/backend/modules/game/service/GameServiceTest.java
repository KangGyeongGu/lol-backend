package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.entity.JudgeStatus;
import com.lol.backend.modules.game.entity.Submission;
import com.lol.backend.modules.game.repo.SubmissionRepository;
import com.lol.backend.modules.shop.service.GameInventoryService;
import com.lol.backend.modules.user.entity.Language;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.snapshot.SnapshotWriter;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * GameService 단위 테스트.
 * 게임 결과 계산 로직 검증 중심.
 */
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameStateStore gameStateStore;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameInventoryService gameInventoryService;

    @Mock
    private SnapshotWriter snapshotWriter;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private GameService gameService;

    private UUID gameId;
    private UUID roomId;
    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;
    private GameStateDto gameStateDto;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();

        gameStateDto = new GameStateDto(
                gameId,
                roomId,
                GameType.RANKED.name(),
                "PLAY",
                Instant.now(),
                Instant.now().plusSeconds(1800),
                Instant.now(),
                null,
                UUID.randomUUID(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("finishGame - RANKED 게임 종료 시 제출 기반 순위 산정 및 차등 보상 적용")
    void finishGame_rankedGame_shouldCalculateRanksAndRewards() {
        // given
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(gameStateDto));

        List<GamePlayerStateDto> players = List.of(
                createGamePlayer(user1Id, 1000),
                createGamePlayer(user2Id, 1000),
                createGamePlayer(user3Id, 1000)
        );
        when(gameStateStore.getGamePlayers(gameId)).thenReturn(players);

        // user1: AC 2개, 최종 제출 5000ms
        // user2: AC 1개, 최종 제출 3000ms
        // user3: 제출 없음
        List<Submission> acSubmissions = List.of(
                createSubmission(gameId, user1Id, 3000),
                createSubmission(gameId, user1Id, 5000),
                createSubmission(gameId, user2Id, 3000)
        );
        when(submissionRepository.findByGameIdAndJudgeStatus(gameId, JudgeStatus.AC))
                .thenReturn(acSubmissions);

        doNothing().when(snapshotWriter).flushGame(gameId);

        // when
        gameService.finishGame(gameId);

        // then
        ArgumentCaptor<GamePlayerStateDto> captor = ArgumentCaptor.forClass(GamePlayerStateDto.class);
        verify(gameStateStore, times(3)).updateGamePlayer(eq(gameId), any(UUID.class), captor.capture());

        List<GamePlayerStateDto> updatedPlayers = captor.getAllValues();

        // user1: 1등 (AC 2개, 5000ms)
        GamePlayerStateDto user1Result = updatedPlayers.stream()
                .filter(p -> p.userId().equals(user1Id))
                .findFirst()
                .orElseThrow();
        assertThat(user1Result.rankInGame()).isEqualTo(1);
        assertThat(user1Result.scoreDelta()).isEqualTo(30);
        assertThat(user1Result.coinDelta()).isEqualTo(100);
        assertThat(user1Result.expDelta()).isEqualTo(50.0);
        assertThat(user1Result.result()).isEqualTo("WIN");
        assertThat(user1Result.solved()).isTrue();

        // user2: 2등 (AC 1개, 3000ms)
        GamePlayerStateDto user2Result = updatedPlayers.stream()
                .filter(p -> p.userId().equals(user2Id))
                .findFirst()
                .orElseThrow();
        assertThat(user2Result.rankInGame()).isEqualTo(2);
        assertThat(user2Result.scoreDelta()).isEqualTo(10);
        assertThat(user2Result.coinDelta()).isEqualTo(50);
        assertThat(user2Result.expDelta()).isEqualTo(30.0);
        assertThat(user2Result.result()).isEqualTo("WIN");
        assertThat(user2Result.solved()).isTrue();

        // user3: 3등 (제출 없음)
        GamePlayerStateDto user3Result = updatedPlayers.stream()
                .filter(p -> p.userId().equals(user3Id))
                .findFirst()
                .orElseThrow();
        assertThat(user3Result.rankInGame()).isEqualTo(3);
        assertThat(user3Result.scoreDelta()).isEqualTo(-10);
        assertThat(user3Result.coinDelta()).isEqualTo(20);
        assertThat(user3Result.expDelta()).isEqualTo(20.0);
        assertThat(user3Result.result()).isEqualTo("LOSE");
        assertThat(user3Result.solved()).isFalse();

        verify(snapshotWriter).flushGame(gameId);
    }

    @Test
    @DisplayName("finishGame - NORMAL 게임 종료 시 scoreDelta=0, 모든 플레이어 DRAW")
    void finishGame_normalGame_shouldNotChangeScore() {
        // given
        GameStateDto normalGame = new GameStateDto(
                gameId,
                roomId,
                GameType.NORMAL.name(),
                "PLAY",
                Instant.now(),
                Instant.now().plusSeconds(1800),
                Instant.now(),
                null,
                UUID.randomUUID(),
                Instant.now()
        );
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(normalGame));

        List<GamePlayerStateDto> players = List.of(
                createGamePlayer(user1Id, 1000),
                createGamePlayer(user2Id, 1000)
        );
        when(gameStateStore.getGamePlayers(gameId)).thenReturn(players);

        List<Submission> acSubmissions = List.of(
                createSubmission(gameId, user1Id, 3000)
        );
        when(submissionRepository.findByGameIdAndJudgeStatus(gameId, JudgeStatus.AC))
                .thenReturn(acSubmissions);

        doNothing().when(snapshotWriter).flushGame(gameId);

        // when
        gameService.finishGame(gameId);

        // then
        ArgumentCaptor<GamePlayerStateDto> captor = ArgumentCaptor.forClass(GamePlayerStateDto.class);
        verify(gameStateStore, times(2)).updateGamePlayer(eq(gameId), any(UUID.class), captor.capture());

        List<GamePlayerStateDto> updatedPlayers = captor.getAllValues();

        // 모든 플레이어 scoreDelta=0, result=DRAW
        updatedPlayers.forEach(player -> {
            assertThat(player.scoreDelta()).isEqualTo(0);
            assertThat(player.result()).isEqualTo("DRAW");
            assertThat(player.coinDelta()).isEqualTo(50);
            assertThat(player.expDelta()).isEqualTo(30.0);
        });
    }

    @Test
    @DisplayName("finishGame - 동점 시 같은 순위 및 DRAW 처리")
    void finishGame_tiedPlayers_shouldHaveSameRankAndDraw() {
        // given
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(gameStateDto));

        List<GamePlayerStateDto> players = List.of(
                createGamePlayer(user1Id, 1000),
                createGamePlayer(user2Id, 1000)
        );
        when(gameStateStore.getGamePlayers(gameId)).thenReturn(players);

        // user1, user2: 모두 AC 1개, 최종 제출 5000ms (동점)
        List<Submission> acSubmissions = List.of(
                createSubmission(gameId, user1Id, 5000),
                createSubmission(gameId, user2Id, 5000)
        );
        when(submissionRepository.findByGameIdAndJudgeStatus(gameId, JudgeStatus.AC))
                .thenReturn(acSubmissions);

        doNothing().when(snapshotWriter).flushGame(gameId);

        // when
        gameService.finishGame(gameId);

        // then
        ArgumentCaptor<GamePlayerStateDto> captor = ArgumentCaptor.forClass(GamePlayerStateDto.class);
        verify(gameStateStore, times(2)).updateGamePlayer(eq(gameId), any(UUID.class), captor.capture());

        List<GamePlayerStateDto> updatedPlayers = captor.getAllValues();

        // 모두 1등, DRAW
        updatedPlayers.forEach(player -> {
            assertThat(player.rankInGame()).isEqualTo(1);
            assertThat(player.result()).isEqualTo("DRAW");
            assertThat(player.solved()).isTrue();
        });
    }

    @Test
    @DisplayName("finishGame - 게임이 존재하지 않으면 예외 발생")
    void finishGame_gameNotFound_shouldThrowException() {
        // given
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gameService.finishGame(gameId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GAME_NOT_FOUND);
    }

    @Test
    @DisplayName("finishGame - 이미 종료된 게임은 예외 발생")
    void finishGame_alreadyFinished_shouldThrowException() {
        // given
        GameStateDto finishedGame = new GameStateDto(
                gameId,
                roomId,
                GameType.RANKED.name(),
                "FINISHED",
                Instant.now(),
                null,
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                Instant.now()
        );
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(finishedGame));

        // when & then
        assertThatThrownBy(() -> gameService.finishGame(gameId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GAME_ALREADY_FINISHED);
    }

    // Helper methods

    private GamePlayerStateDto createGamePlayer(UUID userId, int scoreBefore) {
        return new GamePlayerStateDto(
                UUID.randomUUID(),
                gameId,
                userId,
                "CONNECTED",
                scoreBefore,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                null,
                null
        );
    }

    private Submission createSubmission(UUID gameId, UUID userId, int elapsedMs) {
        return new Submission(
                gameId,
                userId,
                Language.JAVA,
                "public class Solution {}",
                elapsedMs,
                100,
                1024,
                JudgeStatus.AC,
                null,
                1000
        );
    }
}
