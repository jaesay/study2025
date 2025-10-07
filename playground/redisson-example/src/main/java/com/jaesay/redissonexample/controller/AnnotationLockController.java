package com.jaesay.redissonexample.controller;

import com.jaesay.redissonexample.exception.DistributedLockException;
import com.jaesay.redissonexample.model.User;
import com.jaesay.redissonexample.service.AnnotationBasedLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 어노테이션 기반 분산락 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/annotation-lock")
@RequiredArgsConstructor
public class AnnotationLockController {

    private final AnnotationBasedLockService annotationBasedLockService;

    /**
     * 기본 사용자 처리 API
     */
    @PostMapping("/user/{userId}/process")
    public ResponseEntity<String> processUser(@PathVariable String userId) {
        try {
            String result = annotationBasedLockService.processUser(userId);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 재고 감소 API
     */
    @PostMapping("/product/{productId}/stock/decrease")
    public ResponseEntity<String> decreaseStock(@PathVariable String productId,
                                                @RequestParam(defaultValue = "1") int quantity) {
        try {
            String result = annotationBasedLockService.decreaseProductStock(productId, quantity);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 사용자 프로필 업데이트 API
     */
    @PutMapping("/user/profile")
    public ResponseEntity<String> updateProfile(@RequestBody User user) {
        try {
            String result = annotationBasedLockService.updateUserProfile(user);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 주문 처리 API
     */
    @PostMapping("/order")
    public ResponseEntity<String> processOrder(@RequestParam String userId,
                                               @RequestParam String productId,
                                               @RequestParam String action) {
        try {
            String result = annotationBasedLockService.processOrder(userId, productId, action);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 공정한 큐 처리 API
     */
    @PostMapping("/fair-queue/{queueName}/task/{taskId}")
    public ResponseEntity<String> processFairQueue(@PathVariable String queueName,
                                                   @PathVariable String taskId) {
        try {
            String result = annotationBasedLockService.processFairQueue(queueName, taskId);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 설정 읽기 API (읽기 락)
     */
    @GetMapping("/config/{configName}")
    public ResponseEntity<String> readConfig(@PathVariable String configName) {
        try {
            String result = annotationBasedLockService.readConfig(configName);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 설정 업데이트 API (쓰기 락)
     */
    @PutMapping("/config/{configName}")
    public ResponseEntity<String> updateConfig(@PathVariable String configName,
                                               @RequestParam String newValue) {
        try {
            String result = annotationBasedLockService.updateConfig(configName, newValue);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 사용자 타입별 처리 API
     */
    @PostMapping("/user/{userId}/process-by-type")
    public ResponseEntity<String> processUserByType(@PathVariable String userId,
                                                    @RequestParam(defaultValue = "false") boolean isVip) {
        try {
            String result = annotationBasedLockService.processUserByType(userId, isVip);
            return ResponseEntity.ok(result);
        } catch (DistributedLockException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}