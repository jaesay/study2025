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

### 3. 로그 확인 포인트
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
1. **Lock 패턴**: `try-finally`로 안전한 락 관리
2. **락 키 설계**: 비즈니스 로직에 맞는 고유 키 생성
3. **동시성 제어**: 같은 리소스에 대한 동시 접근 방지
4. **자동 만료**: 60초 후 자동 해제로 데드락 방지

---
*이 문서는 학습 과정에서 지속적으로 업데이트됩니다.*