package com.lol.backend.modules.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ShopSpellRequest(
    @NotBlank(message = "spellId는 필수입니다")
    String spellId,

    @Min(value = 1, message = "quantity는 1 이상이어야 합니다")
    int quantity
) {
}
