package com.jaesay.redislockregistryexample.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
@RequiredArgsConstructor
public class RedisLockConfig {

    private final RedisLockProperties redisLockProperties;

    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        RedisLockRegistry registry = new RedisLockRegistry(
                redisConnectionFactory, 
                redisLockProperties.getRegistryKey(), 
                redisLockProperties.getExpireAfter()
        );
        
        // 내부 캐시 용량 설정 (메모리 누수 방지)
        registry.setCacheCapacity(redisLockProperties.getCacheCapacity());
        
        return registry;
    }
}