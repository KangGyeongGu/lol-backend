package com.lol.backend.modules.game.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.modules.game.dto.*;
import com.lol.backend.modules.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 게임 라이프사이클 REST Controller.
 * OPENAPI.yaml.md Games 태그 엔드포인트를 구현한다.
 */
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * GET /games/{gameId}/state
     * 게임 상태 조회
     */
    @GetMapping("/{gameId}/state")
    public ResponseEntity<ApiResponse<GameStateResponse>> getGameState(
            @PathVariable UUID gameId) {

        GameStateResponse result = gameService.getGameState(gameId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    /**
     * POST /games/{gameId}/submissions
     * 코드 제출 (PLAY stage)
     */
    @PostMapping("/{gameId}/submissions")
    public ResponseEntity<ApiResponse<GameStateResponse>> submitCode(
            @PathVariable UUID gameId,
            @RequestBody @Valid SubmissionRequest request) {

        GameStateResponse result = gameService.submitCode(gameId, request);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }
}
