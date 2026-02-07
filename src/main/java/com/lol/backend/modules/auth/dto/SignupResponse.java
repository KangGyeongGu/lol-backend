package com.lol.backend.modules.auth.dto;

import com.lol.backend.modules.user.dto.UserSummaryResponse;

public record SignupResponse(
        String accessToken,
        String refreshToken,
        UserSummaryResponse user
) {
}
