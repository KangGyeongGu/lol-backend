package com.lol.backend.modules.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 게임 stage별 제한시간 설정 (초 단위).
 * application.yml의 game.stage-duration 바인딩.
 */
@ConfigurationProperties(prefix = "game.stage-duration")
public record GameStageProperties(
        long ban,
        long pick,
        long shop,
        long play
) {
}
