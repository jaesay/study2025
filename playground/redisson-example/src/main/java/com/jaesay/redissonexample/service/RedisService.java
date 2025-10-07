package com.jaesay.redissonexample.service;

import com.jaesay.redissonexample.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // String 타입 처리
    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        log.info("Redis에 값 저장 - key: {}, value: {}", key, value);
    }

    public void setValue(String key, String value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
        log.info("Redis에 값 저장 (TTL 설정) - key: {}, value: {}, timeout: {}", key, value, timeout);
    }

    public String getValue(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        log.info("Redis에서 값 조회 - key: {}, value: {}", key, value);
        return value != null ? value.toString() : null;
    }

    // Hash 타입 처리
    public void setHashValue(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
        log.info("Redis Hash에 값 저장 - key: {}, field: {}, value: {}", key, field, value);
    }

    public Object getHashValue(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        log.info("Redis Hash에서 값 조회 - key: {}, field: {}, value: {}", key, field, value);
        return value;
    }

    public Map<Object, Object> getHashValues(String key) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
        log.info("Redis Hash 전체 조회 - key: {}, size: {}", key, values.size());
        return values;
    }

    // List 타입 처리
    public void pushToList(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
        log.info("Redis List에 값 추가 - key: {}, value: {}", key, value);
    }

    public Object popFromList(String key) {
        Object value = redisTemplate.opsForList().leftPop(key);
        log.info("Redis List에서 값 제거 - key: {}, value: {}", key, value);
        return value;
    }

    public List<Object> getListValues(String key) {
        List<Object> values = redisTemplate.opsForList().range(key, 0, -1);
        log.info("Redis List 전체 조회 - key: {}, size: {}", key, values != null ? values.size() : 0);
        return values;
    }

    // Set 타입 처리
    public void addToSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
        log.info("Redis Set에 값 추가 - key: {}, value: {}", key, value);
    }

    public Set<Object> getSetValues(String key) {
        Set<Object> values = redisTemplate.opsForSet().members(key);
        log.info("Redis Set 전체 조회 - key: {}, size: {}", key, values != null ? values.size() : 0);
        return values;
    }

    // 키 존재 여부 확인
    public boolean hasKey(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        log.info("Redis 키 존재 여부 확인 - key: {}, exists: {}", key, exists);
        return Boolean.TRUE.equals(exists);
    }

    // 키 삭제
    public boolean deleteKey(String key) {
        Boolean deleted = redisTemplate.delete(key);
        log.info("Redis 키 삭제 - key: {}, deleted: {}", key, deleted);
        return Boolean.TRUE.equals(deleted);
    }

    // TTL 설정
    public boolean expire(String key, Duration timeout) {
        Boolean result = redisTemplate.expire(key, timeout);
        log.info("Redis 키 TTL 설정 - key: {}, timeout: {}, result: {}", key, timeout, result);
        return Boolean.TRUE.equals(result);
    }

    // 사용자 객체 저장/조회 예시
    public void saveUser(User user) {
        String key = "user:" + user.getId();
        redisTemplate.opsForValue().set(key, user);
        log.info("사용자 정보 저장 - {}", user);
    }

    public User getUser(String userId) {
        String key = "user:" + userId;
        Object user = redisTemplate.opsForValue().get(key);
        log.info("사용자 정보 조회 - userId: {}, user: {}", userId, user);
        return user != null ? (User) user : null;
    }
}