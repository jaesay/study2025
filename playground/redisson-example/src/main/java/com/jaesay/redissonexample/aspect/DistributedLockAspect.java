package com.jaesay.redissonexample.aspect;

import com.jaesay.redissonexample.annotation.DistributedLock;
import com.jaesay.redissonexample.exception.DistributedLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 분산락 어노테이션을 처리하는 AOP Aspect
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 락 키 생성
        String lockKey = generateLockKey(joinPoint, distributedLock.key());
        
        // 락 타입에 따른 락 객체 생성
        RLock lock = createLock(lockKey, distributedLock);
        
        log.info("분산락 시도 - Key: {}, Type: {}, Thread: {}", 
                lockKey, distributedLock.lockType(), Thread.currentThread().getName());

        try {
            // 락 획득 시도
            boolean acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("분산락 획득 실패 - Key: {}, Thread: {}", 
                        lockKey, Thread.currentThread().getName());
                throw new DistributedLockException(distributedLock.failureMessage());
            }

            log.info("분산락 획득 성공 - Key: {}, Thread: {}", 
                    lockKey, Thread.currentThread().getName());

            // 원본 메서드 실행
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            log.error("분산락 처리 중 인터럽트 발생 - Key: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new DistributedLockException("락 처리 중 인터럽트가 발생했습니다.", e);
        } finally {
            // 락 해제 (현재 스레드가 락을 보유하고 있는 경우에만)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("분산락 해제 - Key: {}, Thread: {}", 
                        lockKey, Thread.currentThread().getName());
            }
        }
    }

    /**
     * SPEL을 사용하여 락 키를 동적으로 생성
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, String spelExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 파라미터 이름 가져오기
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        
        if (parameterNames == null) {
            log.warn("메서드 파라미터 이름을 가져올 수 없습니다. 락 키: {}", spelExpression);
            return spelExpression;
        }

        // SPEL 평가 컨텍스트 설정
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        try {
            // SPEL 표현식 파싱 및 평가
            Expression expression = expressionParser.parseExpression(spelExpression);
            Object result = expression.getValue(context);
            return result != null ? result.toString() : spelExpression;
        } catch (Exception e) {
            log.error("SPEL 표현식 파싱 실패. 원본 표현식 사용: {}", spelExpression, e);
            return spelExpression;
        }
    }

    /**
     * 락 타입에 따른 락 객체 생성
     */
    private RLock createLock(String lockKey, DistributedLock distributedLock) {
        return switch (distributedLock.lockType()) {
            case REENTRANT -> redissonClient.getLock(lockKey);
            case FAIR -> redissonClient.getFairLock(lockKey);
            case READ -> {
                RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
                yield readWriteLock.readLock();
            }
            case WRITE -> {
                RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
                yield readWriteLock.writeLock();
            }
        };
    }
}