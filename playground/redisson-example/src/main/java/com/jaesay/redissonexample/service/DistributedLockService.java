package com.jaesay.redissonexample.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    // 기본 분산락 예시
    public String basicLockExample(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 락 획득 시도 (최대 10초 대기, 락 보유 시간 60초)
            boolean acquired = lock.tryLock(10, 60, TimeUnit.SECONDS);
            
            if (acquired) {
                log.info("락 획득 성공: {}", lockKey);
                
                // 비즈니스 로직 실행
                Thread.sleep(5000); // 5초간 작업 시뮬레이션
                
                return "작업 완료 - " + Thread.currentThread().getName();
            } else {
                log.warn("락 획득 실패: {}", lockKey);
                return "락 획득 실패";
            }
        } catch (InterruptedException e) {
            log.error("락 처리 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            return "작업 중단";
        } finally {
            // 락이 현재 스레드에 의해 잠겨있는 경우에만 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("락 해제: {}", lockKey);
            }
        }
    }

    // 재고 감소 시뮬레이션 (동시성 문제 해결)
    public String decreaseStock(String productId, int quantity) {
        String lockKey = "stock:lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 락 획득 (최대 5초 대기, 락 보유 시간 30초)
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            
            if (!acquired) {
                return "재고 처리 중입니다. 잠시 후 다시 시도해주세요.";
            }
            
            log.info("재고 감소 락 획득: {} - {}", productId, Thread.currentThread().getName());
            
            // 현재 재고 조회 (실제로는 DB 조회)
            int currentStock = getCurrentStock(productId);
            
            if (currentStock < quantity) {
                return "재고가 부족합니다. 현재 재고: " + currentStock;
            }
            
            // 재고 감소 처리 (실제로는 DB 업데이트)
            Thread.sleep(1000); // DB 작업 시뮬레이션
            int newStock = currentStock - quantity;
            updateStock(productId, newStock);
            
            log.info("재고 감소 완료: {} - {} -> {}", productId, currentStock, newStock);
            return String.format("재고 감소 성공: %d -> %d", currentStock, newStock);
            
        } catch (InterruptedException e) {
            log.error("재고 감소 처리 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            return "재고 처리 중단";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("재고 감소 락 해제: {}", productId);
            }
        }
    }

    // 공정한 락 예시 (Fair Lock)
    public String fairLockExample(String lockKey) {
        RLock lock = redissonClient.getFairLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (acquired) {
                log.info("공정한 락 획득: {} - {}", lockKey, Thread.currentThread().getName());
                
                // 작업 시뮬레이션
                Thread.sleep(3000);
                
                return "공정한 락 작업 완료 - " + Thread.currentThread().getName();
            } else {
                return "공정한 락 획득 실패";
            }
        } catch (InterruptedException e) {
            log.error("공정한 락 처리 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            return "작업 중단";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("공정한 락 해제: {}", lockKey);
            }
        }
    }

    // 읽기/쓰기 락 예시
    public String readLockExample(String lockKey) {
        RLock readLock = redissonClient.getReadWriteLock(lockKey).readLock();
        
        try {
            boolean acquired = readLock.tryLock(5, 20, TimeUnit.SECONDS);
            
            if (acquired) {
                log.info("읽기 락 획득: {} - {}", lockKey, Thread.currentThread().getName());
                
                // 읽기 작업 시뮬레이션
                Thread.sleep(2000);
                
                return "읽기 작업 완료 - " + Thread.currentThread().getName();
            } else {
                return "읽기 락 획득 실패";
            }
        } catch (InterruptedException e) {
            log.error("읽기 락 처리 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            return "읽기 작업 중단";
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
                log.info("읽기 락 해제: {}", lockKey);
            }
        }
    }

    public String writeLockExample(String lockKey) {
        RLock writeLock = redissonClient.getReadWriteLock(lockKey).writeLock();
        
        try {
            boolean acquired = writeLock.tryLock(5, 20, TimeUnit.SECONDS);
            
            if (acquired) {
                log.info("쓰기 락 획득: {} - {}", lockKey, Thread.currentThread().getName());
                
                // 쓰기 작업 시뮬레이션
                Thread.sleep(3000);
                
                return "쓰기 작업 완료 - " + Thread.currentThread().getName();
            } else {
                return "쓰기 락 획득 실패";
            }
        } catch (InterruptedException e) {
            log.error("쓰기 락 처리 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            return "쓰기 작업 중단";
        } finally {
            if (writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
                log.info("쓰기 락 해제: {}", lockKey);
            }
        }
    }

    // 시뮬레이션용 재고 관리 메서드들 (실제로는 DB 연동)
    private int getCurrentStock(String productId) {
        // 실제로는 DB에서 조회
        // 여기서는 시뮬레이션을 위해 하드코딩
        return 100;
    }
    
    private void updateStock(String productId, int newStock) {
        // 실제로는 DB 업데이트
        log.info("재고 업데이트: {} = {}", productId, newStock);
    }
}