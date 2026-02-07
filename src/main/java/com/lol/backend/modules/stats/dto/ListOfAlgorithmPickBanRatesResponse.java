package com.lol.backend.modules.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 알고리즘 밴/픽률 목록.
 * OPENAPI ListOfAlgorithmPickBanRates 스키마 참조.
 */
public record ListOfAlgorithmPickBanRatesResponse(
        @JsonProperty("items")
        List<AlgorithmPickBanRateResponse> items
) {
    public static ListOfAlgorithmPickBanRatesResponse of(List<AlgorithmPickBanRateResponse> items) {
        return new ListOfAlgorithmPickBanRatesResponse(items);
    }
}
