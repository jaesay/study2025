# RedisLockRegistry 설정 가이드

## 개요
Spring Integration의 RedisLockRegistry를 사용하여 분산 환경에서 Redis 기반 락을 구현하는 방법을 학습합니다.

## 환경 설정

### 1. Redis 서버 설정 (Podman Compose)
```yaml
redis:
    image: redis:7.2
    command: redis-server --requirepass 1a2b3c4d5e!@
    ports:
        - "6379:6379"
    volumes:
        - redis-data:/data
```

### 2. 프로젝트 의존성 (`build.gradle`)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.integration:spring-integration-redis'  // RedisLockRegistry를 위해 필요
    // ... 기타 의존성
}
```

**핵심 포인트**: `spring-integration-redis`가 RedisLockRegistry 사용의 핵심 의존성입니다.

### 3. Redis 연결 설정 (`application.yml`)
```yaml
spring:
    application:
        name: redis-lock-registry-example
    data:
        redis:
            host: localhost
            port: 6379
            password: 1a2b3c4d5e!@
            timeout: 3000ms
            lettuce:
                pool:
                    max-active: 8    # 동시 사용 가능한 최대 연결 수
                    max-idle: 8      # 풀에 유지할 최대 유휴 연결 수
                    min-idle: 0      # 풀에 유지할 최소 유휴 연결 수
```

#### Lettuce 커넥션 풀이 필요한 이유:

**1. 동시성 처리**
- 여러 스레드가 동시에 락을 요청할 때 각각 Redis 연결 필요
- 예시: 온라인 쇼핑몰에서 동시에 발생하는 상황
  ```java
  // Thread 1: 주문 처리
  Lock orderLock = redisLockRegistry.obtain("order-123");
  
  // Thread 2: 재고 업데이트  
  Lock inventoryLock = redisLockRegistry.obtain("inventory-item-456");
  
  // Thread 3: 결제 처리
  Lock paymentLock = redisLockRegistry.obtain("payment-user-789");
  ```

**2. 성능 최적화**
- 연결 생성/해제 오버헤드 감소로 락 응답 속도 향상
- **시나리오**: 1초에 100건의 락 요청이 있을 때
  - 풀 없이: 매번 새 연결 생성 → 100ms 지연
  - 풀 사용: 기존 연결 재사용 → 5ms 지연

**3. 리소스 관리**
- 과도한 연결 생성 방지 및 메모리 사용량 제어
- **실제 예시**: 
  ```
  max-active: 8 설정 시
  ├── 동시 요청 10개 → 8개는 즉시 처리, 2개는 대기
  ├── 메모리 사용량 예측 가능
  └── Redis 서버 부하 제어
  ```

**4. 장애 상황 대응**
- 네트워크 지연이나 Redis 일시 장애 시에도 안정적인 락 처리
- 커넥션 풀이 없으면 모든 요청이 타임아웃까지 기다려야 함

### 4. RedisLockRegistry 설정 클래스
`src/main/java/com/jaesay/redislockregistryexample/config/RedisLockConfig.java`

```java
@Configuration
public class RedisLockConfig {
    
    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockRegistry(redisConnectionFactory, "locks:", 60000);
    }
}
```

**설정 파라미터 설명:**
- `redisConnectionFactory`: Spring이 자동 주입하는 Redis 연결 팩토리
- `"locks:"`: Redis 키 접두사 (락 키가 `locks:order-123` 형태로 저장됨)
- `60000`: 락 만료 시간 60초 (데드락 방지, 애플리케이션 장애 시 자동 해제)

## 실제 사용 예제

### 1. 주문 처리 서비스 (`OrderService.java`)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final RedisLockRegistry redisLockRegistry;
    
    public String processOrder(String userId, String productId, int quantity) {
        String lockKey = "order:" + userId + ":" + productId;
        Lock lock = redisLockRegistry.obtain(lockKey);
        
        try {
            lock.lock();  // 분산 락 획득
            log.info("락 획득 완료: {}", lockKey);
            
            // 비즈니스 로직 실행 (2초 시뮬레이션)
            Thread.sleep(2000);
            
            String orderId = "ORD-" + System.currentTimeMillis();
            return "주문 완료: " + orderId;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "주문 처리 실패: 인터럽트";
        } finally {
            lock.unlock();  // 반드시 락 해제
            log.info("락 해제 완료: {}", lockKey);
        }
    }
}
```

### 2. REST API 컨트롤러 (`OrderController.java`)
```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
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
}
```

## 테스트 방법

### 1. 애플리케이션 실행
```bash
# Redis 서버 시작
podman-compose up -d

# Spring Boot 애플리케이션 실행
./gradlew bootRun
```

### 2. API 테스트

**단일 주문 처리:**
```bash
curl -X POST "http://localhost:8080/api/orders/process?userId=user1&productId=product1&quantity=2"
```

**동시성 테스트:**
```bash
curl "http://localhost:8080/api/orders/test-concurrent?userId=user1&productId=product1"
```

### 3. HTTP 클라이언트 테스트 (IntelliJ)
`_http/order-api.http` 파일 사용:
- 단일 주문 처리 테스트
- 동시성 테스트 (같은 락 키 vs 다른 락 키)
- 다양한 사용자/상품 조합 테스트

### 4. 로그 확인 포인트
- `락 획득 완료`: 분산 락이 성공적으로 획득됨
- `락 해제 완료`: 작업 완료 후 락이 정상 해제됨
- 동시 요청 시 순차적으로 처리되는지 확인

## 학습 진행 상황
- [x] 프로젝트 분석 및 의존성 추가
- [x] Redis 연결 설정
- [x] RedisLockRegistry Bean 구성
- [x] 실제 사용 예제 구현
- [x] REST API 및 동시성 테스트 구현

## 주요 학습 포인트

### 1. Lock 패턴
**try-finally를 통한 안전한 락 관리**
```java
try {
    lock.lock();
    // 비즈니스 로직
} finally {
    lock.unlock();  // 반드시 실행됨
}
```

### 2. 락 키 설계
**비즈니스 로직에 맞는 고유 키 생성**
- `"order:" + userId + ":" + productId` - 사용자별, 상품별 분리
- 너무 세분화하면 락의 효과 감소, 너무 광범위하면 성능 저하

### 3. 동시성 제어
**같은 리소스에 대한 동시 접근 방지**
- 같은 락 키: 순차 처리 (예: 같은 사용자의 같은 상품 주문)
- 다른 락 키: 병렬 처리 (예: 다른 사용자 또는 다른 상품)

### 4. 자동 만료
**60초 후 자동 해제로 데드락 방지**
- 애플리케이션 장애 시에도 락이 영구적으로 남지 않음
- 적절한 만료 시간 설정이 중요 (너무 짧으면 작업 중 해제, 너무 길면 복구 지연)

### 5. InterruptedException 처리
**스레드 인터럽트 상황에 대한 적절한 처리**

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // 인터럽트 상태 복원
    log.error("주문 처리 중 인터럽트 발생", e);
    return "주문 처리 실패: 인터럽트";
}
```

**InterruptedException이 발생하는 상황:**
- **Thread.sleep() 중**: 비즈니스 로직 실행 중 스레드 인터럽트
- **애플리케이션 종료**: Spring Boot 종료 시 실행 중인 작업들 정리
- **스레드 풀 종료**: ExecutorService.shutdownNow() 호출 시
- **명시적 인터럽트**: 다른 스레드에서 interrupt() 호출

**Thread.currentThread().interrupt() 호출 이유:**
- InterruptedException catch 시 인터럽트 플래그가 자동으로 클리어됨
- 상위 코드나 스레드 풀이 인터럽트 상태를 확인할 수 있도록 플래그 복원
- Graceful shutdown을 위한 협력적 스레드 관리

**실제 시나리오 예시:**
```
1. 주문 처리 시작 (2초 소요 예정)
2. 1초 후 애플리케이션 종료 명령
3. Thread.sleep(2000) 중에 InterruptedException 발생
4. 인터럽트 상태 복원 후 깔끔하게 종료
```

## 어노테이션 기반 접근법 (V2)

### 1. @DistributedLock 어노테이션
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();                                    // SpEL 표현식으로 락 키 생성
    long waitTime() default 3000L;                   // 락 획득 대기 시간 (ms)
    long leaseTime() default 30000L;                 // 락 리스 시간 (ms)
    String failureMessage() default "분산 락 획득에 실패했습니다";
}
```

### 2. AOP Aspect를 통한 자동 락 처리
```java
@Aspect
@Component
public class DistributedLockAspect {
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        // SpEL로 락 키 생성 → 락 획득 → 메서드 실행 → 락 해제
    }
}
```

### 3. OrderServiceV2 - 깔끔한 비즈니스 로직
```java
@Service
public class OrderServiceV2 {
    
    @DistributedLock(
        key = "'order:' + #userId + ':' + #productId",
        waitTime = 3000L,
        failureMessage = "주문 처리 중입니다. 잠시 후 다시 시도해주세요."
    )
    public String processOrder(String userId, String productId, int quantity) {
        // 순수한 비즈니스 로직만!
        log.info("주문 처리 시작 (V2): userId={}, productId={}, quantity={}", userId, productId, quantity);
        Thread.sleep(2000);  // 비즈니스 로직 시뮬레이션
        return "주문 완료 (V2): " + orderId;
    }
}
```

### 4. V1 vs V2 비교

| 측면 | V1 (수동 락 관리) | V2 (어노테이션 기반) |
|------|------------------|---------------------|
| **코드 길이** | ~20줄 (락 관리 포함) | ~10줄 (비즈니스 로직만) |
| **가독성** | 락 코드가 비즈니스 로직과 섞임 | 순수한 비즈니스 로직 |
| **유지보수** | 락 관리 코드 중복 | 중앙화된 락 처리 |
| **예외 처리** | 수동으로 모든 케이스 처리 | Aspect에서 일관성 있게 처리 |
| **설정 유연성** | 하드코딩된 값들 | 어노테이션 파라미터로 설정 |

## 실제 동작 시나리오 분석

### 동시성 테스트 로그 예시
```bash
curl "http://localhost:8080/api/orders/v2/test-concurrent?userId=user1&productId=product1"
```

**로그 타임라인:**
```
00:38:16.398 Thread-5: 분산 락 획득 완료: order:user1:product1
00:38:16.398 Thread-5: 주문 처리 시작 (V2): quantity=2
00:38:18.404 Thread-5: 주문 처리 완료 (V2): ORD-V2-1759937898404  
00:38:18.409 Thread-5: 분산 락 해제 완료: order:user1:product1

00:38:18.412 Thread-6: 분산 락 획득 완료: order:user1:product1  ← Thread-5 해제 직후
00:38:18.412 Thread-6: 주문 처리 시작 (V2): quantity=3
00:38:20.418 Thread-6: 주문 처리 완료 (V2): ORD-V2-1759937900417
00:38:20.425 Thread-6: 분산 락 해제 완료: order:user1:product1

00:38:19.401 Thread-4: 분산 락 획득 실패: order:user1:product1  ← 3초 대기 후 타임아웃
Thread-4 실패: 주문 처리 중입니다. 잠시 후 다시 시도해주세요.
```

### 시나리오 해석

**1. 정상적인 순차 처리**
- Thread-5: 16.398 ~ 18.409 (약 2초간 작업)
- Thread-6: 18.412 ~ 20.425 (Thread-5 완료 직후 시작)

**2. 대기 시간 초과로 인한 실패**
- Thread-4: 16.398부터 대기 시작
- `waitTime = 3000L` 설정에 따라 19.401에 타임아웃
- Thread-6가 아직 작업 중이므로 락 획득 불가

**3. 이런 동작이 바람직한 이유**
- ✅ **중복 주문 방지**: 같은 사용자의 같은 상품 주문이 동시에 처리되지 않음
- ✅ **시스템 안정성**: 무한 대기하지 않고 적절한 시간 후 실패
- ✅ **사용자 경험**: 명확한 에러 메시지로 재시도 유도
- ✅ **리소스 보호**: 과도한 동시 요청으로부터 시스템 보호

### 다양한 락 키 패턴의 효과

**동일한 락 키 (순차 처리):**
```java
// 모두 같은 락 키 "order:user1:product1"
processOrder("user1", "product1", 1);  // Thread-1
processOrder("user1", "product1", 2);  // Thread-2 (대기)
processOrder("user1", "product1", 3);  // Thread-3 (대기 또는 실패)
```

**다른 락 키 (병렬 처리):**
```java
// 각각 다른 락 키
processOrder("user1", "product1", 1);  // "order:user1:product1"
processOrder("user2", "product1", 1);  // "order:user2:product1" (병렬 처리)
processOrder("user1", "product2", 1);  // "order:user1:product2" (병렬 처리)
```

---
*이 문서는 학습 과정에서 지속적으로 업데이트됩니다.*