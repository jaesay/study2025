package com.jaesay.redissonexample.controller;

import com.jaesay.redissonexample.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lock")
@RequiredArgsConstructor
public class DistributedLockController {

    private final DistributedLockService distributedLockService;

    // 기본 분산락 테스트
    @PostMapping("/basic/{lockKey}")
    public ResponseEntity<String> basicLock(@PathVariable String lockKey) {
        String result = distributedLockService.basicLockExample(lockKey);
        return ResponseEntity.ok(result);
    }

    // 재고 감소 시뮬레이션
    @PostMapping("/stock/{productId}/decrease")
    public ResponseEntity<String> decreaseStock(@PathVariable String productId, 
                                                @RequestParam(defaultValue = "1") int quantity) {
        String result = distributedLockService.decreaseStock(productId, quantity);
        return ResponseEntity.ok(result);
    }

    // 공정한 락 테스트
    @PostMapping("/fair/{lockKey}")
    public ResponseEntity<String> fairLock(@PathVariable String lockKey) {
        String result = distributedLockService.fairLockExample(lockKey);
        return ResponseEntity.ok(result);
    }

    // 읽기 락 테스트
    @GetMapping("/read/{lockKey}")
    public ResponseEntity<String> readLock(@PathVariable String lockKey) {
        String result = distributedLockService.readLockExample(lockKey);
        return ResponseEntity.ok(result);
    }

    // 쓰기 락 테스트
    @PostMapping("/write/{lockKey}")
    public ResponseEntity<String> writeLock(@PathVariable String lockKey) {
        String result = distributedLockService.writeLockExample(lockKey);
        return ResponseEntity.ok(result);
    }
}