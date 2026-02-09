package com.lol.backend.modules.game.dto;

import jakarta.validation.constraints.NotBlank;

public record BanPickRequest(
    @NotBlank(message = "algorithmId는 필수입니다")
    String algorithmId
) {
}
