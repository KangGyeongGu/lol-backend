package com.lol.backend.modules.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lol.backend.modules.shop.entity.Algorithm;

public record AlgorithmSummaryResponse(
    @JsonProperty("algorithmId")
    String algorithmId,

    @JsonProperty("name")
    String name
) {
    public static AlgorithmSummaryResponse from(Algorithm algorithm) {
        return new AlgorithmSummaryResponse(
            algorithm.getId().toString(),
            algorithm.getName()
        );
    }
}
