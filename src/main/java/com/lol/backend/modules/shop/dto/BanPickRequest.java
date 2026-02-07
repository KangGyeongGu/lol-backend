package com.lol.backend.modules.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record BanPickRequest(
    @NotBlank(message = "algorithmId는 필수입니다")
    String algorithmId
) {
}
