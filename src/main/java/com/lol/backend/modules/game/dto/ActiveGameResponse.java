package com.lol.backend.modules.game.dto;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.state.dto.GameStateDto;

import java.time.Duration;
import java.time.Instant;

public record ActiveGameResponse(
        String gameId,
        String roomId,
        GameStage stage,
        String pageRoute,
        GameType gameType,
        Integer remainingMs
) {
    public static ActiveGameResponse from(Game game) {
        String pageRoute = resolvePageRoute(game.getStage());
        Integer remainingMs = calculateRemainingMs(game);

        return new ActiveGameResponse(
                game.getId().toString(),
                game.getRoomId().toString(),
                game.getStage(),
                pageRoute,
                game.getGameType(),
                remainingMs
        );
    }

    public static ActiveGameResponse from(GameStateDto game) {
        GameStage stage = GameStage.valueOf(game.stage());
        String pageRoute = resolvePageRoute(stage);
        Integer remainingMs = (game.stageDeadlineAt() != null)
                ? (int) Math.max(0, Duration.between(Instant.now(), game.stageDeadlineAt()).toMillis())
                : null;

        return new ActiveGameResponse(
                game.id().toString(),
                game.roomId().toString(),
                stage,
                pageRoute,
                GameType.valueOf(game.gameType()),
                remainingMs
        );
    }

    private static String resolvePageRoute(GameStage stage) {
        return switch (stage) {
            case LOBBY -> "WAITING_ROOM";
            case BAN, PICK, SHOP -> "BAN_PICK_SHOP";
            case PLAY -> "IN_GAME";
            case FINISHED -> "WAITING_ROOM";
        };
    }

    private static Integer calculateRemainingMs(Game game) {
        if (game.getStageDeadlineAt() == null) {
            return null;
        }
        long remaining = Duration.between(Instant.now(), game.getStageDeadlineAt()).toMillis();
        return (int) Math.max(0, remaining);
    }
}
