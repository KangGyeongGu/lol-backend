package com.lol.backend.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ListOfSpellsResponse(
    @JsonProperty("items")
    List<SpellSummaryResponse> items
) {
}
