package com.jaesay.redislockregistryexample.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    
    /**
     * 락 키를 생성하는 SpEL 표현식
     * 예: "'order:' + #userId + ':' + #productId"
     */
    String key();
    
    /**
     * 락 획득 대기 시간 (밀리초)
     * 기본값: 3초
     */
    long waitTime() default 3000L;
    
    /**
     * 락 리스 시간 (밀리초)
     * 기본값: 30초 (RedisLockRegistry 설정보다 짧게)
     */
    long leaseTime() default 30000L;
    
    /**
     * 락 획득 실패 시 예외 메시지
     */
    String failureMessage() default "분산 락 획득에 실패했습니다";
}