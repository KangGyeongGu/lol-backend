package com.lol.backend.state;

import com.lol.backend.config.TestcontainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 인프라 통합 테스트
 * - TestcontainersConfig로 PostgreSQL + Redis 컨테이너 기동
 * - RedisTemplate Bean 로딩 확인
 * - 기본 SET/GET 동작 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class RedisInfraTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void tearDown() {
        // 테스트 간 격리를 위해 모든 키 삭제
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void redisTemplate_beanLoads_successfully() {
        assertThat(redisTemplate).isNotNull();
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
    }

    @Test
    void redis_setAndGet_worksCorrectly() {
        String key = "test:key";
        String value = "test-value";

        redisTemplate.opsForValue().set(key, value);
        String retrieved = redisTemplate.opsForValue().get(key);

        assertThat(retrieved).isEqualTo(value);
    }

    @Test
    void redis_delete_removesKey() {
        String key = "test:delete:key";
        String value = "to-be-deleted";

        redisTemplate.opsForValue().set(key, value);
        assertThat(redisTemplate.hasKey(key)).isTrue();

        redisTemplate.delete(key);
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void redis_hash_operations_workCorrectly() {
        String key = "test:hash";
        String hashKey = "field1";
        String hashValue = "value1";

        redisTemplate.opsForHash().put(key, hashKey, hashValue);
        Object retrieved = redisTemplate.opsForHash().get(key, hashKey);

        assertThat(retrieved).isEqualTo(hashValue);
    }

    @Test
    void redis_multipleKeys_canBeStored() {
        redisTemplate.opsForValue().set("test:key1", "value1");
        redisTemplate.opsForValue().set("test:key2", "value2");
        redisTemplate.opsForValue().set("test:key3", "value3");

        assertThat(redisTemplate.opsForValue().get("test:key1")).isEqualTo("value1");
        assertThat(redisTemplate.opsForValue().get("test:key2")).isEqualTo("value2");
        assertThat(redisTemplate.opsForValue().get("test:key3")).isEqualTo("value3");
    }
}
