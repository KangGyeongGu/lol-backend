package com.lol.backend.modules.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.game.dto.GamePlayerResponse;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.game.dto.SubmissionRequest;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.service.GameService;
import com.lol.backend.modules.user.entity.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

@WebMvcTest(GameController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    private static final UUID GAME_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private GameStateResponse sampleGameStateResponse() {
        return new GameStateResponse(
                GAME_ID.toString(),
                "room-1",
                GameType.RANKED,
                GameStage.PLAY,
                60000L,
                List.of(
                        new GamePlayerResponse("user-1", "player1", 1500),
                        new GamePlayerResponse("user-2", "player2", 1400)
                ),
                500,
                InventoryResponse.empty()
        );
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void getGameState_success() throws Exception {
        GameStateResponse response = sampleGameStateResponse();

        when(gameService.getGameState(GAME_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value(GAME_ID.toString()))
                .andExpect(jsonPath("$.data.roomId").value("room-1"))
                .andExpect(jsonPath("$.data.gameType").value("RANKED"))
                .andExpect(jsonPath("$.data.stage").value("PLAY"))
                .andExpect(jsonPath("$.data.remainingMs").value(60000))
                .andExpect(jsonPath("$.data.players").isArray())
                .andExpect(jsonPath("$.data.players.length()").value(2))
                .andExpect(jsonPath("$.data.coin").value(500))
                .andExpect(jsonPath("$.data.inventory").exists())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void submitCode_success() throws Exception {
        GameStateResponse response = sampleGameStateResponse();
        SubmissionRequest request = new SubmissionRequest(Language.JAVA, "public class Solution {}");

        when(gameService.submitCode(eq(GAME_ID), any(SubmissionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/games/{gameId}/submissions", GAME_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value(GAME_ID.toString()))
                .andExpect(jsonPath("$.data.stage").value("PLAY"))
                .andExpect(jsonPath("$.meta").exists());
    }
}
