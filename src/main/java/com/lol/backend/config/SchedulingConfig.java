package com.lol.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정.
 * - @Scheduled 애너테이션을 활성화한다.
 * - EffectExpirationScheduler에서 1초마다 만료된 효과를 체크한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
