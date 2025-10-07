package com.jaesay.redissonexample.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산락을 적용하기 위한 어노테이션
 * 메서드 레벨에서 사용하며, SPEL을 통해 동적으로 락 키를 생성할 수 있습니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키를 생성하기 위한 SPEL 표현식
     * 예시: 
     * - "'user:' + #userId" 
     * - "'product:' + #productId + ':stock'"
     * - "#user.id + ':' + #action"
     */
    String key();

    /**
     * 락 획득을 위한 최대 대기 시간 (기본값: 5초)
     */
    long waitTime() default 5L;

    /**
     * 락을 보유할 최대 시간 (기본값: 30초)
     * 이 시간이 지나면 자동으로 락이 해제됩니다.
     */
    long leaseTime() default 30L;

    /**
     * 시간 단위 (기본값: SECONDS)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 락 획득 실패 시 예외 메시지
     */
    String failureMessage() default "락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.";

    /**
     * 공정한 락(Fair Lock) 사용 여부 (기본값: false)
     * true로 설정하면 FIFO 순서로 락을 획득합니다.
     */
    boolean fairLock() default false;

    /**
     * 락 타입 (기본값: REENTRANT)
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 락 타입 열거형
     */
    enum LockType {
        REENTRANT,    // 재진입 가능한 락 (기본)
        FAIR,         // 공정한 락
        READ,         // 읽기 락
        WRITE         // 쓰기 락
    }
}