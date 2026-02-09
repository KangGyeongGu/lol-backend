package com.lol.backend.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ListOfItemsResponse(
    @JsonProperty("items")
    List<ItemSummaryResponse> items
) {
}
