package com.jaesay.redislockregistryexample.service;

import com.jaesay.redislockregistryexample.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderServiceV2 {
    
    @DistributedLock(
        key = "'order:' + #userId + ':' + #productId",
        waitTime = 3000L,
        failureMessage = "주문 처리 중입니다. 잠시 후 다시 시도해주세요."
    )
    public String processOrder(String userId, String productId, int quantity) {
        log.info("주문 처리 시작 (V2): userId={}, productId={}, quantity={}", userId, productId, quantity);
        
        try {
            // 실제 비즈니스 로직 시뮬레이션 (2초 소요)
            Thread.sleep(2000);
            
            String orderId = "ORD-V2-" + System.currentTimeMillis();
            log.info("주문 처리 완료 (V2): orderId={}", orderId);
            
            return "주문 완료 (V2): " + orderId;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("주문 처리 중 인터럽트 발생 (V2)", e);
            throw new RuntimeException("주문 처리 실패: 인터럽트", e);
        }
    }
}