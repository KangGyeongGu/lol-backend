package com.lol.backend.state.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.state.EphemeralStateStore;
import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.dto.ConnectionHeartbeatDto;
import com.lol.backend.state.dto.ItemEffectActiveDto;
import com.lol.backend.state.dto.TypingStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis 기반 Ephemeral 상태 저장소 구현체.
 * - TYPING_STATUS: Redis String + TTL (~5초)
 * - CONNECTION_HEARTBEAT: Redis String + TTL (~30초)
 * - ITEM_EFFECT_ACTIVE: Redis String + TTL (아이템 지속 시간)
 */
@Service
public class RedisEphemeralStateStore implements EphemeralStateStore {

    private static final Logger log = LoggerFactory.getLogger(RedisEphemeralStateStore.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEphemeralStateStore(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveTypingStatus(TypingStatusDto typingStatus, Duration ttl) {
        String key = RedisKeyBuilder.typing(typingStatus.roomId(), typingStatus.userId());
        try {
            String json = objectMapper.writeValueAsString(typingStatus);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Saved typing status: roomId={}, userId={}, isTyping={}, ttl={}s",
                    typingStatus.roomId(), typingStatus.userId(), typingStatus.isTyping(), ttl.getSeconds());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize typing status: {}", typingStatus, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "타이핑 상태 저장 실패");
        }
    }

    @Override
    public Optional<TypingStatusDto> getTypingStatus(UUID roomId, UUID userId) {
        String key = RedisKeyBuilder.typing(roomId, userId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            TypingStatusDto dto = objectMapper.readValue(json, TypingStatusDto.class);
            return Optional.of(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize typing status: key={}, json={}", key, json, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveHeartbeat(ConnectionHeartbeatDto heartbeat, Duration ttl) {
        String key = RedisKeyBuilder.heartbeat(heartbeat.userId());
        try {
            String json = objectMapper.writeValueAsString(heartbeat);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Saved heartbeat: userId={}, connectionState={}, ttl={}s",
                    heartbeat.userId(), heartbeat.connectionState(), ttl.getSeconds());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize heartbeat: {}", heartbeat, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "하트비트 저장 실패");
        }
    }

    @Override
    public Optional<ConnectionHeartbeatDto> getHeartbeat(UUID userId) {
        String key = RedisKeyBuilder.heartbeat(userId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            ConnectionHeartbeatDto dto = objectMapper.readValue(json, ConnectionHeartbeatDto.class);
            return Optional.of(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize heartbeat: key={}, json={}", key, json, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveEffect(ItemEffectActiveDto effect, Duration ttl) {
        String key = RedisKeyBuilder.effect(effect.gameId(), effect.uniqueId());
        try {
            String json = objectMapper.writeValueAsString(effect);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Saved effect: gameId={}, uniqueId={}, itemId={}, ttl={}s",
                    effect.gameId(), effect.uniqueId(), effect.itemId(), ttl.getSeconds());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize effect: {}", effect, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "이펙트 저장 실패");
        }
    }

    @Override
    public List<ItemEffectActiveDto> getActiveEffects(UUID gameId) {
        // T2-2에서 구체적으로 구현 예정
        // effect:<gameId>:* 패턴으로 모든 활성 이펙트를 조회
        String pattern = "effect:" + gameId + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(json -> json != null)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ItemEffectActiveDto.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize effect: json={}", json, e);
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Override
    public void removeEffect(UUID gameId, String uniqueId) {
        String key = RedisKeyBuilder.effect(gameId, uniqueId);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Removed effect: gameId={}, uniqueId={}, deleted={}", gameId, uniqueId, deleted);
    }
}
