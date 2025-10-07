package com.jaesay.redissonexample.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class DistributedLockServiceTest {

    @Autowired
    private DistributedLockService distributedLockService;

    @Test
    void testBasicLock() {
        // Given
        String lockKey = "test:basic:lock";

        // When
        String result = distributedLockService.basicLockExample(lockKey);

        // Then
        assertThat(result).contains("작업 완료");
    }

    @Test
    void testConcurrentBasicLock() throws Exception {
        // Given
        String lockKey = "test:concurrent:basic";
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // When
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(
            () -> distributedLockService.basicLockExample(lockKey), executor);
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(
            () -> distributedLockService.basicLockExample(lockKey), executor);
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(
            () -> distributedLockService.basicLockExample(lockKey), executor);

        CompletableFuture.allOf(future1, future2, future3).get();

        String result1 = future1.get();
        String result2 = future2.get();
        String result3 = future3.get();

        // Then
        // 하나는 성공, 나머지는 실패해야 함
        int successCount = 0;
        if (result1.contains("작업 완료")) successCount++;
        if (result2.contains("작업 완료")) successCount++;
        if (result3.contains("작업 완료")) successCount++;

        assertThat(successCount).isEqualTo(1);

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void testStockDecrease() {
        // Given
        String productId = "product123";
        int quantity = 5;

        // When
        String result = distributedLockService.decreaseStock(productId, quantity);

        // Then
        assertThat(result).contains("재고 감소 성공");
    }

    @Test
    void testConcurrentStockDecrease() throws Exception {
        // Given
        String productId = "product456";
        int quantity = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // When
        CompletableFuture<String>[] futures = new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            futures[i] = CompletableFuture.supplyAsync(
                () -> distributedLockService.decreaseStock(productId, quantity), executor);
        }

        CompletableFuture.allOf(futures).get();

        // Then
        int successCount = 0;
        for (CompletableFuture<String> future : futures) {
            String result = future.get();
            if (result.contains("재고 감소 성공")) {
                successCount++;
            }
        }

        // 재고가 100이므로 10씩 5번은 불가능, 일부만 성공해야 함
        assertThat(successCount).isLessThan(5);

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void testFairLock() {
        // Given
        String lockKey = "test:fair:lock";

        // When
        String result = distributedLockService.fairLockExample(lockKey);

        // Then
        assertThat(result).contains("공정한 락 작업 완료");
    }

    @Test
    void testReadLock() {
        // Given
        String lockKey = "test:read:lock";

        // When
        String result = distributedLockService.readLockExample(lockKey);

        // Then
        assertThat(result).contains("읽기 작업 완료");
    }

    @Test
    void testWriteLock() {
        // Given
        String lockKey = "test:write:lock";

        // When
        String result = distributedLockService.writeLockExample(lockKey);

        // Then
        assertThat(result).contains("쓰기 작업 완료");
    }

    @Test
    void testConcurrentReadLocks() throws Exception {
        // Given
        String lockKey = "test:concurrent:read";
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // When - 여러 읽기 락은 동시에 획득 가능해야 함
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(
            () -> distributedLockService.readLockExample(lockKey), executor);
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(
            () -> distributedLockService.readLockExample(lockKey), executor);
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(
            () -> distributedLockService.readLockExample(lockKey), executor);

        CompletableFuture.allOf(future1, future2, future3).get();

        String result1 = future1.get();
        String result2 = future2.get();
        String result3 = future3.get();

        // Then - 모든 읽기 작업이 성공해야 함
        assertThat(result1).contains("읽기 작업 완료");
        assertThat(result2).contains("읽기 작업 완료");
        assertThat(result3).contains("읽기 작업 완료");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
}