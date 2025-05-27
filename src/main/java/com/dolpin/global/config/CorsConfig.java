package com.dolpin.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 프론트엔드 애플리케이션의 도메인 허용
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("https://localhost:3000");
        config.addAllowedOrigin("https://test.dev.dolpin.site:3000");
        config.addAllowedOrigin("https://dolpin.site");
        config.addAllowedOrigin("https://fe.dev.dolpin.site");

        // 필요한 HTTP 메서드 허용
        config.addAllowedMethod("*");

        // 필요한 헤더 허용
        config.addAllowedHeader("*");

        // 인증 정보(쿠키) 허용
        config.setAllowCredentials(true);

        // 노출할 헤더 설정 (쿠키 관련 헤더 포함)
        config.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));

        config.setMaxAge(3600L);

        // 모든 API 경로에 CORS 설정 적용
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}