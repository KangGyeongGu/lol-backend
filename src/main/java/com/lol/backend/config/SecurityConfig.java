package com.lol.backend.config;

import com.lol.backend.common.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 * AUTH_GUARDS.md 규칙에 따라 인증/권한을 처리한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API이므로)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (JWT 기반)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증/인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // AUTH_GUARDS.md 2. 인증 필수 규칙 - 예외 처리
                        .requestMatchers("/api/v1/auth/kakao/login").permitAll()
                        .requestMatchers("/api/v1/auth/signup").permitAll()

                        // 그 외 /api/v1/** 는 인증 필요
                        .requestMatchers("/api/v1/**").authenticated()

                        // 나머지는 허용 (health check 등)
                        .anyRequest().permitAll()
                )

                // 인증 실패 처리
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(authenticationEntryPoint)
                )

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
