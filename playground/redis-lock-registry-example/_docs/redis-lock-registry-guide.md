# RedisLockRegistry 설정 가이드

## RedisLockRegistry란?

**RedisLockRegistry**는 **Spring Integration** 프로젝트의 일부로, Redis를 활용한 분산 락(Distributed Lock) 구현체입니다.

### 프로젝트 관계 및 위치
```
Spring Portfolio
├── Spring Framework (Core)
├── Spring Boot (Auto-configuration)
├── Spring Data Redis (Redis 데이터 접근)
│   ├── Lettuce (기본 Redis 클라이언트)
│   └── Jedis (대안 Redis 클라이언트)
└── Spring Integration (메시징 및 통합)
    └── RedisLockRegistry (분산 락 구현체) ← 여기!
```

### 주요 특징
- ✅ **Spring 공식 프로젝트**: VMware(구 Pivotal)에서 공식 관리
- ✅ **Spring 생태계 완벽 통합**: 설정, DI, 예외 처리 등 자동화
- ✅ **Lettuce 기반**: Spring Data Redis의 기본 클라이언트 활용
- ✅ **간단한 API**: Java의 `java.util.concurrent.locks.Lock` 인터페이스 구현
- ⚠️ **Watchdog 설정 필요**: Spring Integration 6.4+에서 지원하지만 TaskScheduler 명시적 설정 필요

### 다른 분산 락 솔루션과의 비교

| 특징 | RedisLockRegistry | Redisson | Zookeeper |
|------|------------------|----------|-----------|
| **관리 주체** | Spring 공식 | Redisson 팀 | Apache |
| **Spring 통합** | 완벽 통합 | 수동 설정 필요 | 수동 설정 필요 |
| **리스 연장** | TaskScheduler 설정 시 가능 | Watchdog 자동 연장 (기본) | 세션 기반 |
| **복잡도** | 간단 | 중간 | 복잡 |
| **의존성** | Spring Integration | Redisson | Zookeeper |

### 언제 사용하면 좋을까?
- ✅ Spring 기반 애플리케이션
- ✅ 이미 Redis를 사용 중인 환경
- ✅ 간단하고 예측 가능한 작업 시간
- ✅ 학습 및 프로토타이핑

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

## Watchdog 기능 활용 가이드

### Watchdog가 필요한 대표적인 활용처

#### 🚨 Watchdog 필수 케이스

**1. 배치 처리 작업**
```java
@DistributedLock(key = "'batch:' + #jobType + ':' + #date")
public void processDailyBatch(String jobType, String date) {
    // 데이터량에 따라 1분~3시간까지 소요 가능
    List<Order> orders = orderRepository.findByDate(date);
    
    for (Order order : orders) {
        processOrder(order);
        updateInventory(order);
        sendNotification(order);
    }
}
```

**2. 파일 업로드/다운로드**
```java
@DistributedLock(key = "'file:' + #fileId")
public void processLargeFile(String fileId) {
    // 파일 크기에 따라 수초~수시간 소요
    File file = downloadFromS3(fileId);
    processFile(file);          // 이미지 리사이징, 비디오 인코딩 등
    uploadToDestination(file);
}
```

**3. 외부 API 호출이 많은 작업**
```java
@DistributedLock(key = "'integration:' + #customerId")
public void syncCustomerData(String customerId) {
    // 여러 외부 시스템과 통신 (응답 시간 예측 불가)
    CustomerInfo info = crmService.getCustomer(customerId);     // 1-10초
    PaymentInfo payment = paymentService.getHistory(customerId); // 1-30초
    ShippingInfo shipping = shippingService.getStatus(customerId); // 1-60초
    
    mergeAndSave(info, payment, shipping);
}
```

**4. 데이터 마이그레이션**
```java
@DistributedLock(key = "'migration:' + #tableName")
public void migrateTable(String tableName) {
    // 테이블 크기에 따라 몇 분~몇 시간 소요
    List<OldEntity> oldData = oldRepository.findAll();
    
    for (OldEntity old : oldData) {
        NewEntity newEntity = convertToNew(old);
        newRepository.save(newEntity);
    }
}
```

**5. 리포트 생성**
```java
@DistributedLock(key = "'report:' + #reportType + ':' + #period")
public void generateReport(String reportType, String period) {
    // 복잡한 집계 쿼리와 계산 (1-30분 소요)
    ReportData data = analyticsService.aggregateData(period);
    Chart chart = chartService.generateChart(data);
    PDF pdf = pdfService.createReport(chart);
    emailService.sendReport(pdf);
}
```

#### ✅ Watchdog 불필요 케이스

**1. 간단한 CRUD 작업**
```java
@DistributedLock(key = "'order:' + #userId + ':' + #productId")
public String createOrder(String userId, String productId) {
    // 예측 가능한 짧은 작업 (1-3초)
    Order order = new Order(userId, productId);
    return orderRepository.save(order).getId();
}
```

**2. 캐시 업데이트**
```java
@DistributedLock(key = "'cache:' + #key")
public void updateCache(String key, Object value) {
    // 매우 빠른 작업 (밀리초 단위)
    redisTemplate.opsForValue().set(key, value);
}
```

**3. 중복 방지용 락**
```java
@DistributedLock(key = "'duplicate:' + #requestId")
public String processPayment(String requestId, PaymentInfo info) {
    // 중복 처리 방지가 목적 (처리 시간 < 5초)
    return paymentService.process(info);
}
```

### Watchdog 활성화 방법

**TaskScheduler 설정:**
```java
@Configuration
public class RedisLockConfig {
    
    @Bean
    public TaskScheduler lockRenewalScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("lock-renewal-");
        scheduler.initialize();
        return scheduler;
    }
    
    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory,
                                              TaskScheduler lockRenewalScheduler) {
        RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, "locks:", 120000);
        
        // Watchdog 활성화 (자동 리스 연장)
        registry.setRenewalTaskScheduler(lockRenewalScheduler);
        
        return registry;
    }
}
```

**Watchdog 동작 방식:**
- 만료 시간의 1/3마다 자동 연장 (예: 120초 락 → 40초마다 연장)
- 애플리케이션이 살아있는 동안 무한정 연장
- 락 해제 시 또는 애플리케이션 종료 시 자동 중단

### 판단 기준

**✅ Watchdog 필요:**
- 작업 시간이 1분 이상 소요될 가능성
- 외부 시스템 의존성이 높음
- 데이터량에 따라 처리 시간이 크게 달라짐
- 네트워크 I/O가 많음

**❌ Watchdog 불필요:**
- 작업 시간이 10초 이내로 예측 가능
- 메모리 내 연산 위주
- 단순한 데이터베이스 CRUD
- 중복 방지가 주목적

**현재 프로젝트 (주문 처리):** 2초 소요 → Watchdog 불필요 ✅

### 실무 권장사항

**1. 보수적 접근**
```java
// 예상 시간의 3-5배로 설정
예상 작업 시간: 30초 → 락 만료 시간: 120-150초
```

**2. 모니터링 추가**
```java
@DistributedLock(key = "'monitored-job:' + #jobId")
public void monitoredJob(String jobId) {
    long startTime = System.currentTimeMillis();
    try {
        performTask();
    } finally {
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Job {} completed in {} ms", jobId, elapsed);
        
        // 경고: 락 만료 시간의 80% 이상 소요된 경우
        if (elapsed > LOCK_EXPIRY * 0.8) {
            log.warn("Job {} took {}% of lock expiry time", 
                     jobId, (elapsed * 100 / LOCK_EXPIRY));
        }
    }
}
```

**3. 단계별 접근**
- Phase 1: 고정 만료 시간으로 시작하여 운영 데이터 수집
- Phase 2: 필요한 경우 Watchdog 추가
- 확신이 서지 않으면 Watchdog 사용 (안전한 선택)

## 멀티 파드 스케줄 잡에서의 활용

### 일반적인 시나리오
```java
// Kubernetes 환경의 여러 파드에서 1분마다 실행되는 배치 잡
@Scheduled(fixedRate = 60000) // 1분 주기
@DistributedLock(
    key = "'daily-report'",
    waitTime = 2000L  // 이미 실행 중이면 2초 후 포기
)
public void generateDailyReport() {
    // 작업 시간: 보통 30초, 최대 5분 (데이터량에 따라 변동)
    performReportGeneration();
}
```

### 고정 만료 시간의 문제점

**문제 1: 만료 시간을 길게 설정 (10분)**
```
Pod-1: 30초만에 작업 완료 → 락은 10분간 유지
Pod-2, 3, 4: 9분 30초 동안 불필요하게 대기 😢
```

**문제 2: 만료 시간을 짧게 설정 (2분)**
```
Pod-1: 5분 작업 중 → 2분 후 락 만료
Pod-2: 락 획득하여 동시 실행 → 데이터 충돌 위험 🚨
```

### Watchdog 솔루션

**설정:**
```java
@Bean
public RedisLockRegistry redisLockRegistry(RedisConnectionFactory factory,
                                          TaskScheduler scheduler) {
    RedisLockRegistry registry = new RedisLockRegistry(factory, "jobs:", 300000);
    registry.setRenewalTaskScheduler(scheduler); // Watchdog 활성화
    return registry;
}
```

**동작 방식:**
- ✅ **작업 완료 시**: 즉시 락 해제 → 다음 파드가 바로 실행 가능
- ✅ **장시간 작업**: Watchdog가 자동 연장 → 안전하게 완료까지 보호
- ✅ **중복 방지**: 한 번에 하나의 파드에서만 실행

### 실제 동작 예시

**빠른 완료 시나리오 (30초):**
```
00:00:00 Pod-1: 락 획득, 작업 시작
00:00:30 Pod-1: 작업 완료, 락 즉시 해제 ✅
00:01:00 Pod-2: 새로운 스케줄, 락 획득 성공 ✅
```

**긴 작업 시나리오 (5분):**
```
00:00:00 Pod-1: 락 획득, 작업 시작
00:01:00 Pod-2: 락 시도 → 2초 후 포기 ✅
00:02:00 Pod-3: 락 시도 → 2초 후 포기 ✅  
00:03:00 Pod-1: Watchdog 자동 연장 (계속 작업)
00:05:00 Pod-1: 작업 완료, 락 해제 ✅
00:05:00 이후: 다른 파드가 즉시 실행 가능
```

### 권장 설정

**스케줄 잡 최적화:**
```java
@DistributedLock(
    key = "'job:' + #jobName",
    waitTime = 1000L,  // 짧게 설정 (빠른 포기)
    failureMessage = "작업이 이미 실행 중입니다"
)
```

**로깅 추가:**
```java
@Scheduled(fixedRate = 60000)
@DistributedLock(key = "'daily-batch'", waitTime = 2000L)
public void dailyBatch() {
    String podName = System.getenv("HOSTNAME");
    log.info("배치 작업 시작 - Pod: {}", podName);
    
    try {
        performBatch();
        log.info("배치 작업 완료 - Pod: {}", podName);
    } catch (Exception e) {
        log.error("배치 작업 실패 - Pod: {}", podName, e);
        throw e;
    }
}
```

### 핵심 장점

✅ **완벽한 중복 방지**: 여러 파드 중 하나에서만 실행
✅ **효율적인 자원 활용**: 작업 완료 즉시 다음 실행 가능  
✅ **장시간 작업 보호**: 예상보다 오래 걸려도 안전
✅ **운영 안정성**: 예측 불가능한 작업 시간에도 대응

**결론**: 멀티 파드 스케줄 잡에서는 Watchdog 사용이 최적의 선택입니다! 🎯

---
*이 문서는 학습 과정에서 지속적으로 업데이트됩니다.*