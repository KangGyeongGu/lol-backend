package com.lol.backend.modules.game.scheduler;

import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.service.GameService;
import com.lol.backend.state.GameStateStore;
import com.lol.backend.state.dto.GameStateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * GameStageScheduler 단위 테스트.
 * - LOBBY 상태 게임의 자동 전이
 * - deadline 도달 시 stage 전이
 * - PLAY deadline 도달 시 게임 종료
 */
@ExtendWith(MockitoExtension.class)
class GameStageSchedulerTest {

    @Mock
    private GameStateStore gameStateStore;

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameStageScheduler scheduler;

    private UUID gameId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        roomId = UUID.randomUUID();
    }

    @Test
    void checkStageTransitions_noActiveGames_shouldDoNothing() {
        // Given
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of());

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameStateStore, times(1)).getAllActiveGameIds();
        verify(gameStateStore, never()).getGame(any());
        verify(gameService, never()).transitionStage(any(), any());
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_lobbyRankedGame_shouldTransitionToBan() {
        // Given
        GameStateDto lobbyGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.LOBBY.name(),
                null, null, Instant.now(), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(lobbyGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId, GameStage.BAN);
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_lobbyNormalGame_shouldTransitionToPlay() {
        // Given
        GameStateDto lobbyGame = new GameStateDto(
                gameId, roomId, GameType.NORMAL.name(), GameStage.LOBBY.name(),
                null, null, Instant.now(), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(lobbyGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId, GameStage.PLAY);
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_banDeadlineReached_shouldTransitionToPick() {
        // Given
        Instant pastDeadline = Instant.now().minusSeconds(10);
        GameStateDto banGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.BAN.name(),
                Instant.now().minusSeconds(70), pastDeadline, Instant.now().minusSeconds(70), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(banGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId, GameStage.PICK);
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_pickDeadlineReached_shouldTransitionToShop() {
        // Given
        Instant pastDeadline = Instant.now().minusSeconds(5);
        GameStateDto pickGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.PICK.name(),
                Instant.now().minusSeconds(65), pastDeadline, Instant.now().minusSeconds(130), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(pickGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId, GameStage.SHOP);
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_shopDeadlineReached_shouldTransitionToPlay() {
        // Given
        Instant pastDeadline = Instant.now().minusSeconds(1);
        GameStateDto shopGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.SHOP.name(),
                Instant.now().minusSeconds(121), pastDeadline, Instant.now().minusSeconds(251), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(shopGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId, GameStage.PLAY);
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_playDeadlineReached_shouldFinishGame() {
        // Given
        Instant pastDeadline = Instant.now().minusSeconds(10);
        GameStateDto playGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.PLAY.name(),
                Instant.now().minusSeconds(1810), pastDeadline, Instant.now().minusSeconds(2931), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(playGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).finishGame(gameId);
        verify(gameService, never()).transitionStage(any(), any());
    }

    @Test
    void checkStageTransitions_playDeadlineNotReached_shouldDoNothing() {
        // Given
        Instant futureDeadline = Instant.now().plusSeconds(300);
        GameStateDto playGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.PLAY.name(),
                Instant.now().minusSeconds(1500), futureDeadline, Instant.now().minusSeconds(2621), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(playGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, never()).transitionStage(any(), any());
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_finishedGame_shouldSkip() {
        // Given
        GameStateDto finishedGame = new GameStateDto(
                gameId, roomId, GameType.RANKED.name(), GameStage.FINISHED.name(),
                null, null, Instant.now().minusSeconds(3600), Instant.now(), null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(finishedGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, never()).transitionStage(any(), any());
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_normalGamePlayDeadlineReached_shouldFinishGame() {
        // Given
        Instant pastDeadline = Instant.now().minusSeconds(5);
        GameStateDto playGame = new GameStateDto(
                gameId, roomId, GameType.NORMAL.name(), GameStage.PLAY.name(),
                Instant.now().minusSeconds(1805), pastDeadline, Instant.now().minusSeconds(1805), null, null, Instant.now()
        );
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.of(playGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).finishGame(gameId);
        verify(gameService, never()).transitionStage(any(), any());
    }

    @Test
    void checkStageTransitions_gameStateNotFound_shouldSkip() {
        // Given
        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId));
        when(gameStateStore.getGame(gameId)).thenReturn(Optional.empty());

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, never()).transitionStage(any(), any());
        verify(gameService, never()).finishGame(any());
    }

    @Test
    void checkStageTransitions_multipleGames_shouldProcessAll() {
        // Given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();
        UUID gameId3 = UUID.randomUUID();

        GameStateDto lobbyGame = new GameStateDto(
                gameId1, UUID.randomUUID(), GameType.RANKED.name(), GameStage.LOBBY.name(),
                null, null, Instant.now(), null, null, Instant.now()
        );
        GameStateDto banGame = new GameStateDto(
                gameId2, UUID.randomUUID(), GameType.RANKED.name(), GameStage.BAN.name(),
                Instant.now().minusSeconds(70), Instant.now().minusSeconds(10), Instant.now().minusSeconds(70), null, null, Instant.now()
        );
        GameStateDto playGame = new GameStateDto(
                gameId3, UUID.randomUUID(), GameType.NORMAL.name(), GameStage.PLAY.name(),
                Instant.now().minusSeconds(1810), Instant.now().minusSeconds(10), Instant.now().minusSeconds(1810), null, null, Instant.now()
        );

        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId1, gameId2, gameId3));
        when(gameStateStore.getGame(gameId1)).thenReturn(Optional.of(lobbyGame));
        when(gameStateStore.getGame(gameId2)).thenReturn(Optional.of(banGame));
        when(gameStateStore.getGame(gameId3)).thenReturn(Optional.of(playGame));

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId1, GameStage.BAN);
        verify(gameService, times(1)).transitionStage(gameId2, GameStage.PICK);
        verify(gameService, times(1)).finishGame(gameId3);
    }

    @Test
    void checkStageTransitions_exceptionInOneGame_shouldContinueProcessingOthers() {
        // Given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();

        GameStateDto lobbyGame = new GameStateDto(
                gameId1, UUID.randomUUID(), GameType.RANKED.name(), GameStage.LOBBY.name(),
                null, null, Instant.now(), null, null, Instant.now()
        );
        GameStateDto playGame = new GameStateDto(
                gameId2, UUID.randomUUID(), GameType.NORMAL.name(), GameStage.PLAY.name(),
                Instant.now().minusSeconds(1810), Instant.now().minusSeconds(10), Instant.now().minusSeconds(1810), null, null, Instant.now()
        );

        when(gameStateStore.getAllActiveGameIds()).thenReturn(List.of(gameId1, gameId2));
        when(gameStateStore.getGame(gameId1)).thenReturn(Optional.of(lobbyGame));
        when(gameStateStore.getGame(gameId2)).thenReturn(Optional.of(playGame));
        doThrow(new RuntimeException("Test exception")).when(gameService).transitionStage(eq(gameId1), any());

        // When
        scheduler.checkStageTransitions();

        // Then
        verify(gameService, times(1)).transitionStage(gameId1, GameStage.BAN); // throws exception
        verify(gameService, times(1)).finishGame(gameId2); // should still be processed
    }
}
