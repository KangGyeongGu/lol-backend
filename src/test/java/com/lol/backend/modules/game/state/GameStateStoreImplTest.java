package com.lol.backend.modules.game.state;

import com.lol.backend.config.TestcontainersConfig;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfig.class)
class GameStateStoreImplTest {

    @Autowired
    private GameStateStore gameStateStore;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void saveGame_getGame_shouldReturnSavedGame() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant now = Instant.now();

        GameStateDto gameState = new GameStateDto(
                gameId,
                roomId,
                "RANKED",
                "BAN",
                now,
                now.plusSeconds(60),
                now,
                null,
                null,
                now
        );

        // when
        gameStateStore.saveGame(gameState);
        Optional<GameStateDto> retrieved = gameStateStore.getGame(gameId);

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().id()).isEqualTo(gameId);
        assertThat(retrieved.get().roomId()).isEqualTo(roomId);
        assertThat(retrieved.get().gameType()).isEqualTo("RANKED");
        assertThat(retrieved.get().stage()).isEqualTo("BAN");
    }

    @Test
    void getGame_notExists_shouldReturnEmpty() {
        // given
        UUID gameId = UUID.randomUUID();

        // when
        Optional<GameStateDto> result = gameStateStore.getGame(gameId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void saveGamePlayer_getGamePlayer_shouldReturnSavedPlayer() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        GamePlayerStateDto playerState = new GamePlayerStateDto(
                UUID.randomUUID(),
                gameId,
                userId,
                "ACTIVE",
                1500,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null,
                null
        );

        // when
        gameStateStore.saveGamePlayer(playerState);
        Optional<GamePlayerStateDto> retrieved = gameStateStore.getGamePlayer(gameId, userId);

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().gameId()).isEqualTo(gameId);
        assertThat(retrieved.get().userId()).isEqualTo(userId);
        assertThat(retrieved.get().state()).isEqualTo("ACTIVE");
        assertThat(retrieved.get().scoreBefore()).isEqualTo(1500);
    }

    @Test
    void getGamePlayer_notExists_shouldReturnEmpty() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when
        Optional<GamePlayerStateDto> result = gameStateStore.getGamePlayer(gameId, userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void getGamePlayers_shouldReturnAllPlayersInGame() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Instant now = Instant.now();

        GamePlayerStateDto player1 = new GamePlayerStateDto(
                UUID.randomUUID(),
                gameId,
                userId1,
                "ACTIVE",
                1500,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null,
                null
        );

        GamePlayerStateDto player2 = new GamePlayerStateDto(
                UUID.randomUUID(),
                gameId,
                userId2,
                "ACTIVE",
                1400,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null,
                null
        );

        // when
        gameStateStore.saveGamePlayer(player1);
        gameStateStore.saveGamePlayer(player2);
        List<GamePlayerStateDto> players = gameStateStore.getGamePlayers(gameId);

        // then
        assertThat(players).hasSize(2);
        assertThat(players).extracting(GamePlayerStateDto::userId)
                .containsExactlyInAnyOrder(userId1, userId2);
    }

    @Test
    void getGamePlayers_emptyGame_shouldReturnEmptyList() {
        // given
        UUID gameId = UUID.randomUUID();

        // when
        List<GamePlayerStateDto> players = gameStateStore.getGamePlayers(gameId);

        // then
        assertThat(players).isEmpty();
    }

    @Test
    void updateGamePlayer_shouldUpdateExistingPlayer() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        GamePlayerStateDto originalPlayer = new GamePlayerStateDto(
                UUID.randomUUID(),
                gameId,
                userId,
                "ACTIVE",
                1500,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null,
                null
        );

        gameStateStore.saveGamePlayer(originalPlayer);

        // when - 점수 정산 후 업데이트
        GamePlayerStateDto updatedPlayer = new GamePlayerStateDto(
                originalPlayer.id(),
                gameId,
                userId,
                "ACTIVE",
                1500,
                1510,
                10,
                100,
                1,
                true,
                "WIN",
                50,
                100.0,
                now,
                null,
                null
        );

        gameStateStore.updateGamePlayer(gameId, userId, updatedPlayer);
        Optional<GamePlayerStateDto> retrieved = gameStateStore.getGamePlayer(gameId, userId);

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().scoreAfter()).isEqualTo(1510);
        assertThat(retrieved.get().scoreDelta()).isEqualTo(10);
        assertThat(retrieved.get().finalScoreValue()).isEqualTo(100);
        assertThat(retrieved.get().rankInGame()).isEqualTo(1);
        assertThat(retrieved.get().solved()).isTrue();
        assertThat(retrieved.get().result()).isEqualTo("WIN");
        assertThat(retrieved.get().coinDelta()).isEqualTo(50);
        assertThat(retrieved.get().expDelta()).isEqualTo(100.0);
    }

    @Test
    void updateGameStage_shouldUpdateStageAndTimestamps() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant now = Instant.now();

        GameStateDto originalGame = new GameStateDto(
                gameId,
                roomId,
                "RANKED",
                "BAN",
                now,
                now.plusSeconds(60),
                now,
                null,
                null,
                now
        );

        gameStateStore.saveGame(originalGame);

        // when - BAN -> PICK 전이
        Instant pickStarted = now.plusSeconds(60);
        Instant pickDeadline = pickStarted.plusSeconds(60);
        gameStateStore.updateGameStage(gameId, "PICK", pickStarted, pickDeadline);

        Optional<GameStateDto> retrieved = gameStateStore.getGame(gameId);

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().stage()).isEqualTo("PICK");
        assertThat(retrieved.get().stageStartedAt()).isEqualTo(pickStarted);
        assertThat(retrieved.get().stageDeadlineAt()).isEqualTo(pickDeadline);
        // 게임 시작 시간은 변경되지 않아야 함
        assertThat(retrieved.get().startedAt()).isEqualTo(now);
    }

    @Test
    void updateGameStage_nonExistentGame_shouldNotThrowException() {
        // given
        UUID gameId = UUID.randomUUID();
        Instant now = Instant.now();

        // when - 존재하지 않는 게임의 stage 업데이트 시도
        gameStateStore.updateGameStage(gameId, "PICK", now, now.plusSeconds(60));

        // then - 예외 없이 종료되어야 함
        Optional<GameStateDto> result = gameStateStore.getGame(gameId);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteGame_shouldRemoveGameAndPlayers() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        GameStateDto gameState = new GameStateDto(
                gameId,
                roomId,
                "RANKED",
                "FINISHED",
                now,
                null,
                now,
                now.plusSeconds(1800),
                UUID.randomUUID(),
                now
        );

        GamePlayerStateDto playerState = new GamePlayerStateDto(
                UUID.randomUUID(),
                gameId,
                userId,
                "ACTIVE",
                1500,
                1510,
                10,
                null,
                null,
                null,
                "DRAW",
                50,
                100.0,
                now,
                null,
                null
        );

        gameStateStore.saveGame(gameState);
        gameStateStore.saveGamePlayer(playerState);

        // when
        gameStateStore.deleteGame(gameId);

        // then - 게임과 플레이어 모두 삭제되어야 함
        assertThat(gameStateStore.getGame(gameId)).isEmpty();
        assertThat(gameStateStore.getGamePlayer(gameId, userId)).isEmpty();
        assertThat(gameStateStore.getGamePlayers(gameId)).isEmpty();
    }

    @Test
    void getAllActiveGameIds_shouldReturnAllGameIds() {
        // given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();
        UUID gameId3 = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant now = Instant.now();

        GameStateDto game1 = new GameStateDto(
                gameId1, roomId, "RANKED", "BAN", now, now.plusSeconds(60),
                now, null, null, now
        );
        GameStateDto game2 = new GameStateDto(
                gameId2, roomId, "CASUAL", "PICK", now, now.plusSeconds(60),
                now, null, null, now
        );
        GameStateDto game3 = new GameStateDto(
                gameId3, roomId, "RANKED", "PLAY", now, now.plusSeconds(1800),
                now, null, null, now
        );

        gameStateStore.saveGame(game1);
        gameStateStore.saveGame(game2);
        gameStateStore.saveGame(game3);

        // when
        List<UUID> activeGameIds = gameStateStore.getAllActiveGameIds();

        // then
        assertThat(activeGameIds).hasSize(3);
        assertThat(activeGameIds).containsExactlyInAnyOrder(gameId1, gameId2, gameId3);
    }

    @Test
    void getAllActiveGameIds_noGames_shouldReturnEmptyList() {
        // when
        List<UUID> activeGameIds = gameStateStore.getAllActiveGameIds();

        // then
        assertThat(activeGameIds).isEmpty();
    }

    @Test
    void getAllActiveGameIds_shouldIgnoreNonGameKeys() {
        // given
        UUID gameId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant now = Instant.now();

        GameStateDto game = new GameStateDto(
                gameId, roomId, "RANKED", "PLAY", now, now.plusSeconds(1800),
                now, null, null, now
        );

        gameStateStore.saveGame(game);

        // Redis에 다른 타입의 키 추가 (room, effect 등)
        redisTemplate.opsForValue().set("room:some-uuid", "dummy");
        redisTemplate.opsForValue().set("effect:some-uuid:123", "dummy");
        redisTemplate.opsForValue().set("game:invalid-format", "dummy");

        // when
        List<UUID> activeGameIds = gameStateStore.getAllActiveGameIds();

        // then - game:<uuid> 형식의 키만 반환되어야 함
        assertThat(activeGameIds).hasSize(1);
        assertThat(activeGameIds).containsExactly(gameId);
    }
}
