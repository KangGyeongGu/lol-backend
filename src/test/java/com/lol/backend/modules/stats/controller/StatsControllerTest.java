package com.lol.backend.modules.stats.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.stats.dto.AlgorithmPickBanRateResponse;
import com.lol.backend.modules.stats.dto.ListOfAlgorithmPickBanRatesResponse;
import com.lol.backend.modules.stats.dto.ListOfPlayerRankingsResponse;
import com.lol.backend.modules.stats.dto.PlayerRankingResponse;
import com.lol.backend.modules.stats.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(StatsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void getPlayerRankings_success() throws Exception {
        ListOfPlayerRankingsResponse response = ListOfPlayerRankingsResponse.of(List.of(
                PlayerRankingResponse.of(1, "user-1", "player1", 3200, "Challenger"),
                PlayerRankingResponse.of(2, "user-2", "player2", 3100, "Grandmaster")
        ));

        when(statsService.getPlayerRankings()).thenReturn(response);

        mockMvc.perform(get("/api/v1/stats/realtime/player-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].nickname").value("player1"))
                .andExpect(jsonPath("$.data.items[0].tier").value("Challenger"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void getAlgorithmPickBanRates_success() throws Exception {
        ListOfAlgorithmPickBanRatesResponse response = ListOfAlgorithmPickBanRatesResponse.of(List.of(
                AlgorithmPickBanRateResponse.of("algo-1", "BFS", 0.75, 0.1),
                AlgorithmPickBanRateResponse.of("algo-2", "DFS", 0.60, 0.2)
        ));

        when(statsService.getAlgorithmPickBanRates()).thenReturn(response);

        mockMvc.perform(get("/api/v1/stats/realtime/algorithm-pick-ban-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].algorithmId").value("algo-1"))
                .andExpect(jsonPath("$.data.items[0].name").value("BFS"))
                .andExpect(jsonPath("$.data.items[0].pickRate").value(0.75))
                .andExpect(jsonPath("$.data.items[0].banRate").value(0.1))
                .andExpect(jsonPath("$.meta").exists());
    }
}
