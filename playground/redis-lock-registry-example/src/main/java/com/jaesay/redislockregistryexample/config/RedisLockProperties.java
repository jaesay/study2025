package com.jaesay.redislockregistryexample.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "redis.lock")
@Component
@Data
public class RedisLockProperties {
    
    /**
     * Redis 락 키 접두사
     * 기본값: "locks:"
     */
    private String registryKey = "locks:";
    
    /**
     * 락 만료 시간 (밀리초)
     * 기본값: 60초
     */
    private long expireAfter = 60000L;
    
    /**
     * 내부 캐시 용량 (락 객체 최대 보관 개수)
     * 기본값: 100
     * 설명: 메모리 누수 방지를 위한 로컬 ReentrantLock 캐시 크기
     */
    private int cacheCapacity = 100;
    
    /**
     * Watchdog 활성화 여부
     * 기본값: false
     */
    private boolean enableWatchdog = false;
    
    /**
     * Redis 락 타입
     * SPIN_LOCK: 폴링 방식 (기본값)
     * PUB_SUB_LOCK: 이벤트 기반
     */
    private RedisLockType redisLockType = RedisLockType.SPIN_LOCK;
    
    public enum RedisLockType {
        SPIN_LOCK, PUB_SUB_LOCK
    }
}