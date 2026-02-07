package com.lol.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String authorizationCode
) {
}
