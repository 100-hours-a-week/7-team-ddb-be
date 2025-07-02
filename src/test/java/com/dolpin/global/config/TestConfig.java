package com.dolpin.global.config;

import com.dolpin.domain.auth.service.cache.RefreshTokenCacheService;
import com.dolpin.global.redis.service.RedisService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public RedisService mockRedisService() {
        return Mockito.mock(RedisService.class);
    }

    @Bean
    @Primary
    public RefreshTokenCacheService mockRefreshTokenCacheService() {
        return Mockito.mock(RefreshTokenCacheService.class);
    }

    @Bean
    @Primary
    public RedisConnectionFactory mockRedisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> mockRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}
