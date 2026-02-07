package com.lol.backend.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Active Game 응답 DTO.
 * OPENAPI.yaml.md ActiveGame 스키마 참조.
 */
public record ActiveGameResponse(
        @JsonProperty("gameId")
        String gameId,

        @JsonProperty("roomId")
        String roomId,

        @JsonProperty("stage")
        String stage,

        @JsonProperty("pageRoute")
        String pageRoute,

        @JsonProperty("gameType")
        String gameType,

        @JsonProperty("remainingMs")
        Integer remainingMs
) {
}
