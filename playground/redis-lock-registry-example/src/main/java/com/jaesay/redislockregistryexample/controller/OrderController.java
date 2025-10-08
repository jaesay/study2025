package com.jaesay.redislockregistryexample.controller;

import com.jaesay.redislockregistryexample.service.OrderService;
import com.jaesay.redislockregistryexample.service.OrderServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final OrderServiceV2 orderServiceV2;
    
    @PostMapping("/process")
    public String processOrder(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        
        return orderService.processOrder(userId, productId, quantity);
    }
    
    @GetMapping("/test-concurrent")
    public String testConcurrentOrders(@RequestParam String userId, @RequestParam String productId) {
        // 동시성 테스트를 위한 여러 스레드 실행
        for (int i = 0; i < 3; i++) {
            final int threadNum = i + 1;
            new Thread(() -> {
                String result = orderService.processOrder(userId, productId, threadNum);
                System.out.println("Thread " + threadNum + " 결과: " + result);
            }).start();
        }
        
        return "동시성 테스트 시작 - 로그를 확인하세요";
    }
    
    // ============= V2 API (Annotation-based) =============
    
    @PostMapping("/v2/process")
    public String processOrderV2(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        
        return orderServiceV2.processOrder(userId, productId, quantity);
    }
    
    @GetMapping("/v2/test-concurrent")
    public String testConcurrentOrdersV2(@RequestParam String userId, @RequestParam String productId) {
        // 동시성 테스트를 위한 여러 스레드 실행 (V2)
        for (int i = 0; i < 3; i++) {
            final int threadNum = i + 1;
            new Thread(() -> {
                try {
                    String result = orderServiceV2.processOrder(userId, productId, threadNum);
                    System.out.println("V2 Thread " + threadNum + " 결과: " + result);
                } catch (Exception e) {
                    System.out.println("V2 Thread " + threadNum + " 실패: " + e.getMessage());
                }
            }).start();
        }
        
        return "V2 동시성 테스트 시작 - 로그를 확인하세요";
    }
}