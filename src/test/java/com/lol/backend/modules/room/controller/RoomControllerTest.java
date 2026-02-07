package com.lol.backend.modules.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.dto.*;
import com.lol.backend.modules.room.entity.PlayerState;
import com.lol.backend.modules.room.service.RoomService;
import com.lol.backend.modules.user.dto.UserSummaryResponse;
import com.lol.backend.modules.user.entity.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RoomControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ROOM_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    private void setAuthenticatedUser(String userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getRooms_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        RoomSummaryResponse room = new RoomSummaryResponse(
                ROOM_ID,
                "Test Room",
                GameType.RANKED,
                Language.PYTHON,
                4,
                2,
                "WAITING",
                true,
                Instant.now()
        );

        PagedRoomListResponse response = new PagedRoomListResponse(
                List.of(room),
                null,
                1L
        );

        when(roomService.getRooms(any(UUID.class), isNull(), isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.items[0].roomName").value("Test Room"))
                .andExpect(jsonPath("$.data.items[0].gameType").value("RANKED"))
                .andExpect(jsonPath("$.data.items[0].language").value("PYTHON"))
                .andExpect(jsonPath("$.data.items[0].currentPlayers").value(2))
                .andExpect(jsonPath("$.data.items[0].maxPlayers").value(4))
                .andExpect(jsonPath("$.data.items[0].roomStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.items[0].joinable").value(true))
                .andExpect(jsonPath("$.data.listVersion").value(1))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getRooms_withFilters_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        PagedRoomListResponse response = new PagedRoomListResponse(
                Collections.emptyList(),
                null,
                1L
        );

        when(roomService.getRooms(any(UUID.class), eq("Test"), eq(Language.PYTHON), eq(GameType.RANKED), isNull(), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/rooms")
                        .param("roomName", "Test")
                        .param("language", "PYTHON")
                        .param("gameType", "RANKED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void createRoom_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        CreateRoomRequest request = new CreateRoomRequest(
                "New Room",
                GameType.RANKED,
                Language.JAVA,
                4
        );

        UserSummaryResponse hostUser = new UserSummaryResponse(USER_ID, "Host", "Gold II", 1500);
        RoomPlayerResponse hostPlayer = new RoomPlayerResponse(
                hostUser,
                PlayerState.UNREADY,
                true
        );

        RoomDetailResponse response = new RoomDetailResponse(
                ROOM_ID,
                "New Room",
                GameType.RANKED,
                Language.JAVA,
                4,
                List.of(hostPlayer)
        );

        when(roomService.createRoom(any(UUID.class), any(CreateRoomRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.roomName").value("New Room"))
                .andExpect(jsonPath("$.data.gameType").value("RANKED"))
                .andExpect(jsonPath("$.data.language").value("JAVA"))
                .andExpect(jsonPath("$.data.maxPlayers").value(4))
                .andExpect(jsonPath("$.data.players").isArray())
                .andExpect(jsonPath("$.data.players[0].isHost").value(true))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getRoomDetail_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        UserSummaryResponse hostUser = new UserSummaryResponse(USER_ID, "Host", "Gold II", 1500);
        RoomPlayerResponse hostPlayer = new RoomPlayerResponse(
                hostUser,
                PlayerState.READY,
                true
        );

        RoomDetailResponse response = new RoomDetailResponse(
                ROOM_ID,
                "Test Room",
                GameType.RANKED,
                Language.PYTHON,
                4,
                List.of(hostPlayer)
        );

        when(roomService.getRoomDetail(any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/rooms/{roomId}", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.roomName").value("Test Room"))
                .andExpect(jsonPath("$.data.players").isArray())
                .andExpect(jsonPath("$.data.players[0].user.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.players[0].isHost").value(true))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void joinRoom_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        UserSummaryResponse user = new UserSummaryResponse(USER_ID, "Host", "Gold II", 1500);
        RoomPlayerResponse player = new RoomPlayerResponse(
                user,
                PlayerState.UNREADY,
                false
        );

        RoomDetailResponse response = new RoomDetailResponse(
                ROOM_ID,
                "Test Room",
                GameType.RANKED,
                Language.PYTHON,
                4,
                List.of(player)
        );

        when(roomService.joinRoom(any(UUID.class), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.players").isArray())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void leaveRoom_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/leave", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void ready_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        UserSummaryResponse hostUser = new UserSummaryResponse(USER_ID, "Host", "Gold II", 1500);
        RoomPlayerResponse player = new RoomPlayerResponse(
                hostUser,
                PlayerState.READY,
                true
        );

        RoomDetailResponse response = new RoomDetailResponse(
                ROOM_ID,
                "Test Room",
                GameType.RANKED,
                Language.PYTHON,
                4,
                List.of(player)
        );

        when(roomService.ready(any(UUID.class), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/ready", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.players[0].state").value("READY"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void unready_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        UserSummaryResponse hostUser = new UserSummaryResponse(USER_ID, "Host", "Gold II", 1500);
        RoomPlayerResponse player = new RoomPlayerResponse(
                hostUser,
                PlayerState.UNREADY,
                true
        );

        RoomDetailResponse response = new RoomDetailResponse(
                ROOM_ID,
                "Test Room",
                GameType.RANKED,
                Language.PYTHON,
                4,
                List.of(player)
        );

        when(roomService.unready(any(UUID.class), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/unready", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.players[0].state").value("UNREADY"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void kickPlayer_success() throws Exception {
        setAuthenticatedUser(USER_ID);

        KickRequest request = new KickRequest("33333333-3333-3333-3333-333333333333");

        UserSummaryResponse hostUser = new UserSummaryResponse(USER_ID, "Host", "Gold II", 1500);
        RoomPlayerResponse hostPlayer = new RoomPlayerResponse(
                hostUser,
                PlayerState.READY,
                true
        );

        RoomDetailResponse response = new RoomDetailResponse(
                ROOM_ID,
                "Test Room",
                GameType.RANKED,
                Language.PYTHON,
                4,
                List.of(hostPlayer)
        );

        when(roomService.kickPlayer(any(UUID.class), any(UUID.class), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/kick", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.players").isArray())
                .andExpect(jsonPath("$.meta").exists());
    }
}
