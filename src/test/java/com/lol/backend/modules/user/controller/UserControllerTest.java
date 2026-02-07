package com.lol.backend.modules.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lol.backend.common.dto.PageInfo;
import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.user.dto.MatchSummaryResponse;
import com.lol.backend.modules.user.dto.PagedMatchListResponse;
import com.lol.backend.modules.user.dto.UserProfileResponse;
import com.lol.backend.modules.user.dto.UserStatsResponse;
import com.lol.backend.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @BeforeEach
    void setUp() {
        // SecurityUtil.getCurrentUserId() reads authentication.getPrincipal() and expects a String.
        // @WithMockUser sets principal as UserDetails, so we set up a String principal manually.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(USER_ID, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getMyProfile_success() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                USER_ID, "testNickname", "JAVA", "Gold II", 1500, 200.0, 5000
        );

        when(userService.getMyProfile(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.nickname").value("testNickname"))
                .andExpect(jsonPath("$.data.language").value("JAVA"))
                .andExpect(jsonPath("$.data.tier").value("Gold II"))
                .andExpect(jsonPath("$.data.score").value(1500))
                .andExpect(jsonPath("$.data.coin").value(5000))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getMyActiveGame_success() throws Exception {
        ActiveGameResponse response = new ActiveGameResponse(
                "game-1", "room-1", GameStage.PLAY, "IN_GAME", GameType.RANKED, 30000
        );

        when(userService.getMyActiveGame(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me/active-game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.gameId").value("game-1"))
                .andExpect(jsonPath("$.data.stage").value("PLAY"))
                .andExpect(jsonPath("$.data.pageRoute").value("IN_GAME"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getMyActiveGame_noActiveGame_returnsNullData() throws Exception {
        when(userService.getMyActiveGame(USER_ID)).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/me/active-game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getMyStats_success() throws Exception {
        UserStatsResponse response = new UserStatsResponse(100, 60, 35, 5, 60.0);

        when(userService.getMyStats(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.games").value(100))
                .andExpect(jsonPath("$.data.wins").value(60))
                .andExpect(jsonPath("$.data.losses").value(35))
                .andExpect(jsonPath("$.data.draws").value(5))
                .andExpect(jsonPath("$.data.winRate").value(60.0))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getMyMatches_success() throws Exception {
        PagedMatchListResponse response = new PagedMatchListResponse(
                List.of(
                        new MatchSummaryResponse("match-1", "RANKED", "WIN", 25, "2025-01-01T00:00:00Z"),
                        new MatchSummaryResponse("match-2", "NORMAL", "LOSS", -15, "2025-01-02T00:00:00Z")
                ),
                PageInfo.of(20, "next-cursor-abc")
        );

        when(userService.getMyMatches(eq(USER_ID), any(), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].matchId").value("match-1"))
                .andExpect(jsonPath("$.data.items[0].result").value("WIN"))
                .andExpect(jsonPath("$.data.page.limit").value(20))
                .andExpect(jsonPath("$.data.page.nextCursor").value("next-cursor-abc"))
                .andExpect(jsonPath("$.meta").exists());
    }
}
