package com.jaesay.redissonexample.service;

import com.jaesay.redissonexample.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Test
    void testStringOperations() {
        // Given
        String key = "test:string";
        String value = "test value";

        // When
        redisService.setValue(key, value);
        String retrievedValue = redisService.getValue(key);

        // Then
        assertThat(retrievedValue).isEqualTo(value);

        // Clean up
        redisService.deleteKey(key);
    }

    @Test
    void testStringWithTTL() throws InterruptedException {
        // Given
        String key = "test:ttl";
        String value = "ttl value";
        Duration ttl = Duration.ofSeconds(2);

        // When
        redisService.setValue(key, value, ttl);
        
        // Then
        assertThat(redisService.getValue(key)).isEqualTo(value);
        
        // Wait for TTL to expire
        Thread.sleep(3000);
        assertThat(redisService.getValue(key)).isNull();
    }

    @Test
    void testHashOperations() {
        // Given
        String key = "test:hash";
        String field1 = "field1";
        String field2 = "field2";
        String value1 = "value1";
        String value2 = "value2";

        // When
        redisService.setHashValue(key, field1, value1);
        redisService.setHashValue(key, field2, value2);
        
        // Then
        assertThat(redisService.getHashValue(key, field1)).isEqualTo(value1);
        assertThat(redisService.getHashValue(key, field2)).isEqualTo(value2);
        
        Map<Object, Object> allValues = redisService.getHashValues(key);
        assertThat(allValues).hasSize(2);
        assertThat(allValues).containsEntry(field1, value1);
        assertThat(allValues).containsEntry(field2, value2);

        // Clean up
        redisService.deleteKey(key);
    }

    @Test
    void testListOperations() {
        // Given
        String key = "test:list";
        String value1 = "item1";
        String value2 = "item2";

        // When
        redisService.pushToList(key, value1);
        redisService.pushToList(key, value2);
        
        // Then
        List<Object> allValues = redisService.getListValues(key);
        assertThat(allValues).hasSize(2);
        assertThat(allValues).containsExactly(value1, value2);
        
        Object poppedValue = redisService.popFromList(key);
        assertThat(poppedValue).isEqualTo(value1);
        
        allValues = redisService.getListValues(key);
        assertThat(allValues).hasSize(1);

        // Clean up
        redisService.deleteKey(key);
    }

    @Test
    void testSetOperations() {
        // Given
        String key = "test:set";
        String value1 = "item1";
        String value2 = "item2";

        // When
        redisService.addToSet(key, value1);
        redisService.addToSet(key, value2);
        redisService.addToSet(key, value1); // 중복 추가

        // Then
        Set<Object> allValues = redisService.getSetValues(key);
        assertThat(allValues).hasSize(2); // 중복 제거됨
        assertThat(allValues).contains(value1, value2);

        // Clean up
        redisService.deleteKey(key);
    }

    @Test
    void testUserOperations() {
        // Given
        User user = new User("user1", "John Doe", "john@example.com", 30);

        // When
        redisService.saveUser(user);
        User retrievedUser = redisService.getUser(user.getId());

        // Then
        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getId()).isEqualTo(user.getId());
        assertThat(retrievedUser.getName()).isEqualTo(user.getName());
        assertThat(retrievedUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(retrievedUser.getAge()).isEqualTo(user.getAge());

        // Clean up
        redisService.deleteKey("user:" + user.getId());
    }

    @Test
    void testKeyManagement() {
        // Given
        String key = "test:exists";
        String value = "test value";

        // When & Then
        assertThat(redisService.hasKey(key)).isFalse();
        
        redisService.setValue(key, value);
        assertThat(redisService.hasKey(key)).isTrue();
        
        redisService.deleteKey(key);
        assertThat(redisService.hasKey(key)).isFalse();
    }
}