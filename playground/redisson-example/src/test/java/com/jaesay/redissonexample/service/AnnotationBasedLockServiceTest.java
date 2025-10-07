package com.jaesay.redissonexample.service;

import com.jaesay.redissonexample.exception.DistributedLockException;
import com.jaesay.redissonexample.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class AnnotationBasedLockServiceTest {

    @Autowired
    private AnnotationBasedLockService annotationBasedLockService;

    @Test
    void testProcessUser() {
        // Given
        String userId = "user123";

        // When
        String result = annotationBasedLockService.processUser(userId);

        // Then
        assertThat(result).contains("사용자 " + userId + " 처리 완료");
    }

    @Test
    void testConcurrentProcessUser() throws Exception {
        // Given
        String userId = "user456";
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // When
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(
            () -> annotationBasedLockService.processUser(userId), executor);
        
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            assertThatThrownBy(() -> annotationBasedLockService.processUser(userId))
                .isInstanceOf(DistributedLockException.class);
        }, executor);

        // Then
        String result1 = future1.get();
        future2.get();
        
        assertThat(result1).contains("사용자 " + userId + " 처리 완료");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void testDecreaseProductStock() {
        // Given
        String productId = "product123";
        int quantity = 5;

        // When
        String result = annotationBasedLockService.decreaseProductStock(productId, quantity);

        // Then
        assertThat(result).contains("상품 " + productId + " 재고 " + quantity + "개 감소 완료");
    }

    @Test
    void testUpdateUserProfile() {
        // Given
        User user = new User("user789", "Jane Doe", "jane@example.com", 25);

        // When
        String result = annotationBasedLockService.updateUserProfile(user);

        // Then
        assertThat(result).contains("사용자 " + user.getName() + " 프로필 업데이트 완료");
    }

    @Test
    void testProcessOrder() {
        // Given
        String userId = "user111";
        String productId = "product222";
        String action = "purchase";

        // When
        String result = annotationBasedLockService.processOrder(userId, productId, action);

        // Then
        assertThat(result).contains("주문 처리 완료");
        assertThat(result).contains(userId);
        assertThat(result).contains(productId);
        assertThat(result).contains(action);
    }

    @Test
    void testProcessFairQueue() {
        // Given
        String queueName = "processing-queue";
        String taskId = "task123";

        // When
        String result = annotationBasedLockService.processFairQueue(queueName, taskId);

        // Then
        assertThat(result).contains("큐 " + queueName + "에서 작업 " + taskId + " 처리 완료");
    }

    @Test
    void testReadConfig() {
        // Given
        String configName = "app.config";

        // When
        String result = annotationBasedLockService.readConfig(configName);

        // Then
        assertThat(result).contains("설정 " + configName + " 읽기 완료");
    }

    @Test
    void testUpdateConfig() {
        // Given
        String configName = "app.setting";
        String newValue = "new-value";

        // When
        String result = annotationBasedLockService.updateConfig(configName, newValue);

        // Then
        assertThat(result).contains("설정 " + configName + "를 " + newValue + "로 업데이트 완료");
    }

    @Test
    void testConcurrentReadConfig() throws Exception {
        // Given
        String configName = "shared.config";
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // When - 여러 읽기 락은 동시에 획득 가능해야 함
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(
            () -> annotationBasedLockService.readConfig(configName), executor);
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(
            () -> annotationBasedLockService.readConfig(configName), executor);
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(
            () -> annotationBasedLockService.readConfig(configName), executor);

        CompletableFuture.allOf(future1, future2, future3).get();

        String result1 = future1.get();
        String result2 = future2.get();
        String result3 = future3.get();

        // Then - 모든 읽기 작업이 성공해야 함
        assertThat(result1).contains("설정 " + configName + " 읽기 완료");
        assertThat(result2).contains("설정 " + configName + " 읽기 완료");
        assertThat(result3).contains("설정 " + configName + " 읽기 완료");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void testProcessUserByType() {
        // Given
        String userId = "user999";

        // When & Then - VIP 사용자
        String vipResult = annotationBasedLockService.processUserByType(userId, true);
        assertThat(vipResult).contains("VIP 사용자 " + userId + " 처리 완료");

        // When & Then - 일반 사용자
        String normalResult = annotationBasedLockService.processUserByType(userId, false);
        assertThat(normalResult).contains("일반 사용자 " + userId + " 처리 완료");
    }

    @Test
    void testSpelExpressionWithComplexObject() {
        // Given
        User user = new User("complex123", "Complex User", "complex@test.com", 30);

        // When
        String result = annotationBasedLockService.updateUserProfile(user);

        // Then
        assertThat(result).contains("사용자 " + user.getName() + " 프로필 업데이트 완료");
    }
}