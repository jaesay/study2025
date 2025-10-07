package com.jaesay.redissonexample.exception;

/**
 * 분산락 관련 예외 클래스
 */
public class DistributedLockException extends RuntimeException {

    public DistributedLockException(String message) {
        super(message);
    }

    public DistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }
}