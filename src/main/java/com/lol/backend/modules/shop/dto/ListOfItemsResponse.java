package com.lol.backend.modules.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ListOfItemsResponse(
    @JsonProperty("items")
    List<ItemSummaryResponse> items
) {
}
