package com.lol.backend.modules.user.dto;

import com.lol.backend.modules.user.entity.User;

public record UserSummaryResponse(
        String userId,
        String nickname,
        String tier,
        int score
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId().toString(),
                user.getNickname(),
                user.getTier(),
                user.getScore()
        );
    }
}
