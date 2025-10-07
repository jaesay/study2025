package com.jaesay.redissonexample.controller;

import com.jaesay.redissonexample.model.User;
import com.jaesay.redissonexample.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;

    // String 타입 API
    @PostMapping("/string/{key}")
    public ResponseEntity<String> setValue(@PathVariable String key, @RequestBody String value) {
        redisService.setValue(key, value);
        return ResponseEntity.ok("값이 저장되었습니다.");
    }

    @PostMapping("/string/{key}/ttl/{seconds}")
    public ResponseEntity<String> setValueWithTTL(@PathVariable String key, 
                                                  @RequestBody String value,
                                                  @PathVariable long seconds) {
        redisService.setValue(key, value, Duration.ofSeconds(seconds));
        return ResponseEntity.ok("값이 TTL과 함께 저장되었습니다.");
    }

    @GetMapping("/string/{key}")
    public ResponseEntity<String> getValue(@PathVariable String key) {
        String value = redisService.getValue(key);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }

    // Hash 타입 API
    @PostMapping("/hash/{key}/{field}")
    public ResponseEntity<String> setHashValue(@PathVariable String key, 
                                               @PathVariable String field,
                                               @RequestBody Object value) {
        redisService.setHashValue(key, field, value);
        return ResponseEntity.ok("해시 값이 저장되었습니다.");
    }

    @GetMapping("/hash/{key}/{field}")
    public ResponseEntity<Object> getHashValue(@PathVariable String key, @PathVariable String field) {
        Object value = redisService.getHashValue(key, field);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }

    @GetMapping("/hash/{key}")
    public ResponseEntity<Map<Object, Object>> getHashValues(@PathVariable String key) {
        Map<Object, Object> values = redisService.getHashValues(key);
        return ResponseEntity.ok(values);
    }

    // List 타입 API
    @PostMapping("/list/{key}")
    public ResponseEntity<String> pushToList(@PathVariable String key, @RequestBody Object value) {
        redisService.pushToList(key, value);
        return ResponseEntity.ok("리스트에 값이 추가되었습니다.");
    }

    @DeleteMapping("/list/{key}")
    public ResponseEntity<Object> popFromList(@PathVariable String key) {
        Object value = redisService.popFromList(key);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }

    @GetMapping("/list/{key}")
    public ResponseEntity<List<Object>> getListValues(@PathVariable String key) {
        List<Object> values = redisService.getListValues(key);
        return ResponseEntity.ok(values);
    }

    // Set 타입 API
    @PostMapping("/set/{key}")
    public ResponseEntity<String> addToSet(@PathVariable String key, @RequestBody Object value) {
        redisService.addToSet(key, value);
        return ResponseEntity.ok("셋에 값이 추가되었습니다.");
    }

    @GetMapping("/set/{key}")
    public ResponseEntity<Set<Object>> getSetValues(@PathVariable String key) {
        Set<Object> values = redisService.getSetValues(key);
        return ResponseEntity.ok(values);
    }

    // 키 관리 API
    @GetMapping("/exists/{key}")
    public ResponseEntity<Boolean> hasKey(@PathVariable String key) {
        boolean exists = redisService.hasKey(key);
        return ResponseEntity.ok(exists);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteKey(@PathVariable String key) {
        boolean deleted = redisService.deleteKey(key);
        return deleted ? 
            ResponseEntity.ok("키가 삭제되었습니다.") : 
            ResponseEntity.notFound().build();
    }

    @PostMapping("/{key}/expire/{seconds}")
    public ResponseEntity<String> expire(@PathVariable String key, @PathVariable long seconds) {
        boolean result = redisService.expire(key, Duration.ofSeconds(seconds));
        return result ? 
            ResponseEntity.ok("TTL이 설정되었습니다.") : 
            ResponseEntity.badRequest().body("TTL 설정에 실패했습니다.");
    }

    // 사용자 객체 API
    @PostMapping("/user")
    public ResponseEntity<String> saveUser(@RequestBody User user) {
        redisService.saveUser(user);
        return ResponseEntity.ok("사용자 정보가 저장되었습니다.");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        User user = redisService.getUser(userId);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
}