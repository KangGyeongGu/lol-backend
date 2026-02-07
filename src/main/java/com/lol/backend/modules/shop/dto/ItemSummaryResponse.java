package com.lol.backend.modules.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lol.backend.modules.shop.entity.Item;

public record ItemSummaryResponse(
    @JsonProperty("itemId")
    String itemId,

    @JsonProperty("name")
    String name,

    @JsonProperty("description")
    String description,

    @JsonProperty("durationSec")
    int durationSec,

    @JsonProperty("price")
    int price
) {
    public static ItemSummaryResponse from(Item item) {
        return new ItemSummaryResponse(
            item.getId().toString(),
            item.getName(),
            item.getDescription(),
            item.getDurationSec(),
            item.getPrice()
        );
    }
}
