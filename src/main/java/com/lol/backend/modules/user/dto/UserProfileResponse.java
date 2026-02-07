package com.lol.backend.modules.user.dto;

import com.lol.backend.modules.user.entity.User;

public record UserProfileResponse(
        String userId,
        String nickname,
        String language,
        String tier,
        int score,
        double exp,
        int coin
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getNickname(),
                user.getLanguage().name(),
                user.getTier(),
                user.getScore(),
                user.getExp(),
                user.getCoin()
        );
    }
}
