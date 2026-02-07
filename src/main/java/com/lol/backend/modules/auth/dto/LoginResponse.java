package com.lol.backend.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lol.backend.modules.user.dto.UserSummaryResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String result,
        String accessToken,
        String refreshToken,
        String signupToken,
        UserSummaryResponse user
) {
    public static LoginResponse ok(String accessToken, String refreshToken, UserSummaryResponse user) {
        return new LoginResponse("OK", accessToken, refreshToken, null, user);
    }

    public static LoginResponse signupRequired(String signupToken) {
        return new LoginResponse("SIGNUP_REQUIRED", null, null, signupToken, null);
    }
}
