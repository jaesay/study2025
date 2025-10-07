package com.jaesay.redissonexample.service;

import com.jaesay.redissonexample.annotation.DistributedLock;
import com.jaesay.redissonexample.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 어노테이션 기반 분산락 사용 예시 서비스
 */
@Slf4j
@Service
public class AnnotationBasedLockService {

    /**
     * 기본적인 분산락 사용 예시
     * SPEL로 userId를 키에 포함
     */
    @DistributedLock(key = "'user:lock:' + #userId")
    public String processUser(String userId) {
        log.info("사용자 처리 시작: {}", userId);
        
        try {
            // 비즈니스 로직 시뮬레이션
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("처리 중단", e);
        }
        
        log.info("사용자 처리 완료: {}", userId);
        return "사용자 " + userId + " 처리 완료";
    }

    /**
     * 재고 감소 - 상품ID와 액션을 조합한 키 사용
     */
    @DistributedLock(
        key = "'product:' + #productId + ':stock'",
        waitTime = 10,
        leaseTime = 60,
        timeUnit = TimeUnit.SECONDS,
        failureMessage = "재고 처리가 진행 중입니다. 잠시 후 다시 시도해주세요."
    )
    public String decreaseProductStock(String productId, int quantity) {
        log.info("상품 재고 감소 시작: {} - {}", productId, quantity);
        
        try {
            // 재고 확인 및 감소 로직
            Thread.sleep(2000);
            
            // 실제로는 DB 업데이트
            log.info("재고 감소 처리 완료: {} - {}", productId, quantity);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 처리 중단", e);
        }
        
        return String.format("상품 %s 재고 %d개 감소 완료", productId, quantity);
    }

    /**
     * 사용자 객체의 속성을 사용한 키 생성
     */
    @DistributedLock(
        key = "'user:profile:' + #user.id",
        waitTime = 5,
        leaseTime = 30
    )
    public String updateUserProfile(User user) {
        log.info("사용자 프로필 업데이트 시작: {}", user.getId());
        
        try {
            // 프로필 업데이트 로직
            Thread.sleep(1500);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("프로필 업데이트 중단", e);
        }
        
        log.info("사용자 프로필 업데이트 완료: {}", user.getId());
        return "사용자 " + user.getName() + " 프로필 업데이트 완료";
    }

    /**
     * 복합 키 사용 예시 (여러 파라미터 조합)
     */
    @DistributedLock(
        key = "'order:' + #userId + ':' + #productId + ':' + #action",
        waitTime = 3,
        leaseTime = 20
    )
    public String processOrder(String userId, String productId, String action) {
        log.info("주문 처리 시작: {} - {} - {}", userId, productId, action);
        
        try {
            // 주문 처리 로직
            Thread.sleep(2500);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("주문 처리 중단", e);
        }
        
        log.info("주문 처리 완료: {} - {} - {}", userId, productId, action);
        return String.format("주문 처리 완료: %s가 %s 상품을 %s", userId, productId, action);
    }

    /**
     * 공정한 락(Fair Lock) 사용 예시
     */
    @DistributedLock(
        key = "'fair:queue:' + #queueName",
        lockType = DistributedLock.LockType.FAIR,
        waitTime = 15,
        leaseTime = 10
    )
    public String processFairQueue(String queueName, String taskId) {
        log.info("공정한 큐 처리 시작: {} - {}", queueName, taskId);
        
        try {
            // FIFO 순서로 처리되는 작업
            Thread.sleep(3000);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("큐 처리 중단", e);
        }
        
        log.info("공정한 큐 처리 완료: {} - {}", queueName, taskId);
        return String.format("큐 %s에서 작업 %s 처리 완료", queueName, taskId);
    }

    /**
     * 읽기 락 사용 예시
     */
    @DistributedLock(
        key = "'config:' + #configName",
        lockType = DistributedLock.LockType.READ,
        waitTime = 5,
        leaseTime = 15
    )
    public String readConfig(String configName) {
        log.info("설정 읽기 시작: {}", configName);
        
        try {
            // 설정 읽기 작업 (여러 스레드가 동시에 읽기 가능)
            Thread.sleep(1000);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("설정 읽기 중단", e);
        }
        
        log.info("설정 읽기 완료: {}", configName);
        return "설정 " + configName + " 읽기 완료";
    }

    /**
     * 쓰기 락 사용 예시
     */
    @DistributedLock(
        key = "'config:' + #configName",
        lockType = DistributedLock.LockType.WRITE,
        waitTime = 5,
        leaseTime = 30
    )
    public String updateConfig(String configName, String newValue) {
        log.info("설정 업데이트 시작: {} -> {}", configName, newValue);
        
        try {
            // 설정 업데이트 작업 (배타적 접근 필요)
            Thread.sleep(2000);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("설정 업데이트 중단", e);
        }
        
        log.info("설정 업데이트 완료: {} -> {}", configName, newValue);
        return String.format("설정 %s를 %s로 업데이트 완료", configName, newValue);
    }

    /**
     * 조건부 키 생성 예시 (삼항 연산자 사용)
     */
    @DistributedLock(
        key = "#isVip ? 'vip:user:' + #userId : 'normal:user:' + #userId",
        waitTime = 5,
        leaseTime = 20
    )
    public String processUserByType(String userId, boolean isVip) {
        log.info("사용자 타입별 처리 시작: {} (VIP: {})", userId, isVip);
        
        try {
            // VIP 여부에 따른 다른 처리
            if (isVip) {
                Thread.sleep(1000); // VIP는 빠른 처리
            } else {
                Thread.sleep(3000); // 일반 사용자는 일반 처리
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("사용자 처리 중단", e);
        }
        
        log.info("사용자 타입별 처리 완료: {} (VIP: {})", userId, isVip);
        return String.format("%s 사용자 %s 처리 완료", isVip ? "VIP" : "일반", userId);
    }
}