package com.lol.backend.modules.catalog.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.catalog.dto.AlgorithmSummaryResponse;
import com.lol.backend.modules.catalog.dto.ItemSummaryResponse;
import com.lol.backend.modules.catalog.dto.ListOfAlgorithmsResponse;
import com.lol.backend.modules.catalog.dto.ListOfItemsResponse;
import com.lol.backend.modules.catalog.dto.ListOfSpellsResponse;
import com.lol.backend.modules.catalog.dto.SpellSummaryResponse;
import com.lol.backend.modules.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(CatalogController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void getAlgorithms_success() throws Exception {
        ListOfAlgorithmsResponse response = new ListOfAlgorithmsResponse(List.of(
                new AlgorithmSummaryResponse("algo-1", "BFS"),
                new AlgorithmSummaryResponse("algo-2", "DFS")
        ));

        when(catalogService.getAlgorithms()).thenReturn(response);

        mockMvc.perform(get("/api/v1/catalog/algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].algorithmId").value("algo-1"))
                .andExpect(jsonPath("$.data.items[0].name").value("BFS"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void getItems_success() throws Exception {
        ListOfItemsResponse response = new ListOfItemsResponse(List.of(
                new ItemSummaryResponse("item-1", "Potion", "Heal HP", 30, 100),
                new ItemSummaryResponse("item-2", "Shield", "Block damage", 60, 200)
        ));

        when(catalogService.getItems()).thenReturn(response);

        mockMvc.perform(get("/api/v1/catalog/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].itemId").value("item-1"))
                .andExpect(jsonPath("$.data.items[0].name").value("Potion"))
                .andExpect(jsonPath("$.data.items[0].price").value(100))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void getSpells_success() throws Exception {
        ListOfSpellsResponse response = new ListOfSpellsResponse(List.of(
                new SpellSummaryResponse("spell-1", "Fireball", "Deal fire damage", 10, 150)
        ));

        when(catalogService.getSpells()).thenReturn(response);

        mockMvc.perform(get("/api/v1/catalog/spells"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].spellId").value("spell-1"))
                .andExpect(jsonPath("$.data.items[0].name").value("Fireball"))
                .andExpect(jsonPath("$.data.items[0].price").value(150))
                .andExpect(jsonPath("$.meta").exists());
    }
}
