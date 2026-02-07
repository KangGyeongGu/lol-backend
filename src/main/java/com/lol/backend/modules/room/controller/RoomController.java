package com.lol.backend.modules.room.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.dto.*;
import com.lol.backend.modules.room.service.RoomService;
import com.lol.backend.modules.user.entity.Language;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // GET /rooms
    @GetMapping
    public ResponseEntity<ApiResponse<PagedRoomListResponse>> getRooms(
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) GameType gameType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        PagedRoomListResponse result = roomService.getRooms(userId, roomName, language, gameType, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // POST /rooms
    @PostMapping
    public ResponseEntity<ApiResponse<RoomDetailResponse>> createRoom(
            @RequestBody @Valid CreateRoomRequest request) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        RoomDetailResponse result = roomService.createRoom(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // GET /rooms/{roomId}
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomDetail(
            @PathVariable UUID roomId) {

        RoomDetailResponse result = roomService.getRoomDetail(roomId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // POST /rooms/{roomId}/join
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> joinRoom(
            @PathVariable UUID roomId) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        RoomDetailResponse result = roomService.joinRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // POST /rooms/{roomId}/leave
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Map<String, Object>>> leaveRoom(
            @PathVariable UUID roomId) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        roomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(), RequestContextHolder.getRequestId()));
    }

    // POST /rooms/{roomId}/ready
    @PostMapping("/{roomId}/ready")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> ready(
            @PathVariable UUID roomId) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        RoomDetailResponse result = roomService.ready(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // POST /rooms/{roomId}/unready
    @PostMapping("/{roomId}/unready")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> unready(
            @PathVariable UUID roomId) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        RoomDetailResponse result = roomService.unready(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // POST /rooms/{roomId}/start
    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<ActiveGameResponse>> startGame(
            @PathVariable UUID roomId) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        ActiveGameResponse result = roomService.startGame(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }

    // POST /rooms/{roomId}/kick
    @PostMapping("/{roomId}/kick")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> kickPlayer(
            @PathVariable UUID roomId,
            @RequestBody @Valid KickRequest request) {

        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID targetUserId = UUID.fromString(request.targetUserId());
        RoomDetailResponse result = roomService.kickPlayer(roomId, userId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success(result, RequestContextHolder.getRequestId()));
    }
}
