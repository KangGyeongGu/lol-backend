package com.lol.backend.config;

import com.lol.backend.common.interceptor.RequestIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 글로벌 설정.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;

    public WebMvcConfig(RequestIdInterceptor requestIdInterceptor) {
        this.requestIdInterceptor = requestIdInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000") // 개발환경 기본값
                .allowedMethods("GET", "POST", "PATCH") // CONVENTIONS.md 4. HTTP 메서드 규칙 - PUT/DELETE 미사용
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
