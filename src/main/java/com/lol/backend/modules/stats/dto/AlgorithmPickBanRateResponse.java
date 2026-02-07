package com.lol.backend.modules.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 알고리즘 밴/픽률 정보.
 * OPENAPI AlgorithmPickBanRate 스키마 참조.
 */
public record AlgorithmPickBanRateResponse(
        @JsonProperty("algorithmId")
        String algorithmId,

        @JsonProperty("name")
        String name,

        @JsonProperty("pickRate")
        double pickRate,

        @JsonProperty("banRate")
        double banRate
) {
    public static AlgorithmPickBanRateResponse of(String algorithmId, String name, double pickRate, double banRate) {
        return new AlgorithmPickBanRateResponse(algorithmId, name, pickRate, banRate);
    }
}
