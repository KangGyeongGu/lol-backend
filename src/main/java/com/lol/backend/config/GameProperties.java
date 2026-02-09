package com.lol.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 게임 설정값을 관리하는 Properties 클래스.
 * SSOT: ECONOMY.md 1.2절, GAME_RULES.md 5.1절
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "game")
public class GameProperties {
    private ShopConfig shop = new ShopConfig();

    @Getter
    @Setter
    public static class ShopConfig {
        /**
         * 인게임 SHOP 단계 초기 코인 (ECONOMY.md 1.2절)
         */
        private int initialCoin = 3000;

        /**
         * 아이템 최대 구매 수량 (GAME_RULES.md 5.1절)
         */
        private int maxItemCount = 3;

        /**
         * 스펠 최대 구매 수량 (GAME_RULES.md 5.1절)
         */
        private int maxSpellCount = 2;
    }
}
