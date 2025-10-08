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

## 학습 진행 상황
- [x] 프로젝트 분석 및 의존성 추가
- [x] Redis 연결 설정
- [ ] RedisLockRegistry Bean 구성 (현재 진행 중)
- [ ] 실제 사용 예제 구현
- [ ] 테스트 케이스 작성

## 다음 단계
1. RedisLockRegistry Bean 설정 완료
2. 분산 락을 사용하는 서비스 클래스 구현
3. 동시성 테스트 및 검증

---
*이 문서는 학습 과정에서 지속적으로 업데이트됩니다.*