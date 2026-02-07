package com.lol.backend.modules.stats.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.modules.stats.dto.ListOfAlgorithmPickBanRatesResponse;
import com.lol.backend.modules.stats.dto.ListOfPlayerRankingsResponse;
import com.lol.backend.modules.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계/랭킹 API 컨트롤러.
 * OPENAPI /stats/** 엔드포인트 참조.
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * GET /api/v1/stats/realtime/player-rankings
     * 실시간 플레이어 랭킹 조회.
     */
    @GetMapping("/realtime/player-rankings")
    public ApiResponse<ListOfPlayerRankingsResponse> getPlayerRankings() {
        ListOfPlayerRankingsResponse response = statsService.getPlayerRankings();
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    /**
     * GET /api/v1/stats/realtime/algorithm-pick-ban-rates
     * 실시간 알고리즘 밴/픽률 조회.
     */
    @GetMapping("/realtime/algorithm-pick-ban-rates")
    public ApiResponse<ListOfAlgorithmPickBanRatesResponse> getAlgorithmPickBanRates() {
        ListOfAlgorithmPickBanRatesResponse response = statsService.getAlgorithmPickBanRates();
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }
}
