package com.jaesay.redislockregistryexample.aspect;

import com.jaesay.redislockregistryexample.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {
    
    private final RedisLockRegistry redisLockRegistry;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = generateLockKey(joinPoint, distributedLock.key());
        Lock lock = redisLockRegistry.obtain(lockKey);
        
        boolean acquired = false;
        try {
            // 락 획득 시도
            acquired = lock.tryLock(distributedLock.waitTime(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("분산 락 획득 실패: {}", lockKey);
                throw new RuntimeException(distributedLock.failureMessage() + " - 락 키: " + lockKey);
            }
            
            log.info("분산 락 획득 완료: {}", lockKey);
            
            // 원본 메서드 실행
            return joinPoint.proceed();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생: {}", lockKey, e);
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (acquired) {
                lock.unlock();
                log.info("분산 락 해제 완료: {}", lockKey);
            }
        }
    }
    
    private String generateLockKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        
        // SpEL 컨텍스트 설정
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }
        
        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}