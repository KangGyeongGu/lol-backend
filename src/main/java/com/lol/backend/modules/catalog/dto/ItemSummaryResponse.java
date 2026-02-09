package com.lol.backend.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lol.backend.modules.catalog.entity.Item;

public record ItemSummaryResponse(
    @JsonProperty("itemId")
    String itemId,

    @JsonProperty("name")
    String name,

    @JsonProperty("iconKey")
    String iconKey,

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
            item.getIconKey(),
            item.getDescription(),
            item.getDurationSec(),
            item.getPrice()
        );
    }
}
