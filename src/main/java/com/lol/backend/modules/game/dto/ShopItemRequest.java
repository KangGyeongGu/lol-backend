package com.lol.backend.modules.game.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ShopItemRequest(
    @NotBlank(message = "itemId는 필수입니다")
    String itemId,

    @Min(value = 1, message = "quantity는 1 이상이어야 합니다")
    int quantity
) {
}
