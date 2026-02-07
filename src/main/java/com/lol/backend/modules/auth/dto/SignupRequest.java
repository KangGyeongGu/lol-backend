package com.lol.backend.modules.auth.dto;

import com.lol.backend.modules.user.entity.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String signupToken,
        @NotBlank @Size(min = 1, max = 20) String nickname,
        @NotNull Language language
) {
}
