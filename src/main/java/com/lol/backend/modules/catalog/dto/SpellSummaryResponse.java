package com.lol.backend.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lol.backend.modules.catalog.entity.Spell;

public record SpellSummaryResponse(
    @JsonProperty("spellId")
    String spellId,

    @JsonProperty("name")
    String name,

    @JsonProperty("description")
    String description,

    @JsonProperty("durationSec")
    int durationSec,

    @JsonProperty("price")
    int price
) {
    public static SpellSummaryResponse from(Spell spell) {
        return new SpellSummaryResponse(
            spell.getId().toString(),
            spell.getName(),
            spell.getDescription(),
            spell.getDurationSec(),
            spell.getPrice()
        );
    }
}
