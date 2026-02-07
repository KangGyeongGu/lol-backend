package com.lol.backend.modules.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.game.dto.GamePlayerResponse;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.shop.dto.BanPickRequest;
import com.lol.backend.modules.shop.dto.ShopItemRequest;
import com.lol.backend.modules.shop.dto.ShopSpellRequest;
import com.lol.backend.modules.shop.service.BanPickService;
import com.lol.backend.modules.shop.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BanPickShopController 단위 테스트
 * - POST /api/v1/games/{gameId}/ban
 * - POST /api/v1/games/{gameId}/pick
 * - POST /api/v1/games/{gameId}/shop/items
 * - POST /api/v1/games/{gameId}/shop/spells
 */
@WebMvcTest(BanPickShopController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BanPickShopControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String GAME_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BanPickService banPickService;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @Test
    void submitBan_success() throws Exception {
        // Given
        setAuthentication(USER_ID);
        BanPickRequest request = new BanPickRequest("33333333-3333-3333-3333-333333333333");

        GameStateResponse response = createMockGameStateResponse(GameStage.BAN);
        when(banPickService.submitBan(any(), any(), any(BanPickRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/games/" + GAME_ID + "/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value(GAME_ID))
                .andExpect(jsonPath("$.data.stage").value("BAN"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void submitPick_success() throws Exception {
        // Given
        setAuthentication(USER_ID);
        BanPickRequest request = new BanPickRequest("44444444-4444-4444-4444-444444444444");

        GameStateResponse response = createMockGameStateResponse(GameStage.PICK);
        when(banPickService.submitPick(any(), any(), any(BanPickRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/games/" + GAME_ID + "/pick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value(GAME_ID))
                .andExpect(jsonPath("$.data.stage").value("PICK"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void purchaseItem_success() throws Exception {
        // Given
        setAuthentication(USER_ID);
        ShopItemRequest request = new ShopItemRequest("55555555-5555-5555-5555-555555555555", 1);

        GameStateResponse response = createMockGameStateResponse(GameStage.SHOP);
        when(shopService.purchaseItem(any(), any(), any(ShopItemRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/games/" + GAME_ID + "/shop/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value(GAME_ID))
                .andExpect(jsonPath("$.data.stage").value("SHOP"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void purchaseSpell_success() throws Exception {
        // Given
        setAuthentication(USER_ID);
        ShopSpellRequest request = new ShopSpellRequest("66666666-6666-6666-6666-666666666666", 1);

        GameStateResponse response = createMockGameStateResponse(GameStage.SHOP);
        when(shopService.purchaseSpell(any(), any(), any(ShopSpellRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/games/" + GAME_ID + "/shop/spells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value(GAME_ID))
                .andExpect(jsonPath("$.data.stage").value("SHOP"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void submitPick_withInvalidRequest_returns400() throws Exception {
        // Given
        setAuthentication(USER_ID);
        BanPickRequest invalidRequest = new BanPickRequest(""); // blank algorithmId

        // When & Then
        mockMvc.perform(post("/api/v1/games/" + GAME_ID + "/pick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void purchaseItem_withInvalidQuantity_returns400() throws Exception {
        // Given
        setAuthentication(USER_ID);
        ShopItemRequest invalidRequest = new ShopItemRequest("55555555-5555-5555-5555-555555555555", 0); // invalid quantity

        // When & Then
        mockMvc.perform(post("/api/v1/games/" + GAME_ID + "/shop/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ====== Helper Methods ======

    private void setAuthentication(String userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private GameStateResponse createMockGameStateResponse(GameStage stage) {
        GamePlayerResponse player = new GamePlayerResponse(
                USER_ID,
                "testUser",
                0
        );

        InventoryResponse inventory = new InventoryResponse(
                Collections.emptyList(),
                Collections.emptyList()
        );

        return new GameStateResponse(
                GAME_ID,
                "77777777-7777-7777-7777-777777777777",
                GameType.RANKED,
                stage,
                60000L,
                List.of(player),
                3000,
                inventory
        );
    }
}
