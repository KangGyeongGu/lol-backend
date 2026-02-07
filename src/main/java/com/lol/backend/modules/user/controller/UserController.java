package com.lol.backend.modules.user.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.dto.ActiveGameResponse;
import com.lol.backend.modules.user.dto.PagedMatchListResponse;
import com.lol.backend.modules.user.dto.UserProfileResponse;
import com.lol.backend.modules.user.dto.UserStatsResponse;
import com.lol.backend.modules.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile() {
        String userId = SecurityUtil.getCurrentUserId();
        UserProfileResponse response = userService.getMyProfile(userId);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @GetMapping("/me/active-game")
    public ApiResponse<ActiveGameResponse> getMyActiveGame() {
        String userId = SecurityUtil.getCurrentUserId();
        ActiveGameResponse response = userService.getMyActiveGame(userId);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats() {
        String userId = SecurityUtil.getCurrentUserId();
        UserStatsResponse response = userService.getMyStats(userId);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @GetMapping("/me/matches")
    public ApiResponse<PagedMatchListResponse> getMyMatches(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String userId = SecurityUtil.getCurrentUserId();
        PagedMatchListResponse response = userService.getMyMatches(userId, cursor, limit);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }
}
