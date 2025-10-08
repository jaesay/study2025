package com.jaesay.redislockregistryexample.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final RedisLockRegistry redisLockRegistry;
    
    public String processOrder(String userId, String productId, int quantity) {
        String lockKey = "order:" + userId + ":" + productId;
        Lock lock = redisLockRegistry.obtain(lockKey);
        
        try {
            lock.lock();
            log.info("락 획득 완료: {}", lockKey);
            
            // 재고 확인 및 주문 처리 시뮬레이션
            log.info("주문 처리 시작: userId={}, productId={}, quantity={}", userId, productId, quantity);
            
            // 실제 비즈니스 로직 시뮬레이션 (2초 소요)
            Thread.sleep(2000);
            
            String orderId = "ORD-" + System.currentTimeMillis();
            log.info("주문 처리 완료: orderId={}", orderId);
            
            return "주문 완료: " + orderId;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("주문 처리 중 인터럽트 발생", e);
            return "주문 처리 실패: 인터럽트";
        } finally {
            lock.unlock();
            log.info("락 해제 완료: {}", lockKey);
        }
    }
}