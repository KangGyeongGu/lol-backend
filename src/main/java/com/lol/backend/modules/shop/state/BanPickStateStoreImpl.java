package com.lol.backend.modules.shop.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.state.BanPickStateStore;
import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
public class BanPickStateStoreImpl implements BanPickStateStore {

    private static final Logger log = LoggerFactory.getLogger(BanPickStateStoreImpl.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public BanPickStateStoreImpl(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveBan(GameBanDto ban) {
        String key = RedisKeyBuilder.gameBans(ban.gameId());
        String hashKey = ban.userId().toString();
        try {
            String json = objectMapper.writeValueAsString(ban);
            redisTemplate.opsForHash().put(key, hashKey, json);
            log.debug("Saved ban: gameId={}, userId={}, algorithmId={}", ban.gameId(), ban.userId(), ban.algorithmId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ban: " + ban.id(), e);
        }
    }

    @Override
    public List<GameBanDto> getBans(UUID gameId) {
        String key = RedisKeyBuilder.gameBans(gameId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.values().stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), GameBanDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize ban from gameId={}", gameId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<GameBanDto> getBansByUser(UUID gameId, UUID userId) {
        String key = RedisKeyBuilder.gameBans(gameId);
        String hashKey = userId.toString();
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (value == null) {
            return Collections.emptyList();
        }
        try {
            GameBanDto ban = objectMapper.readValue(value.toString(), GameBanDto.class);
            return List.of(ban);
        } catch (IOException e) {
            log.error("Failed to deserialize ban: gameId={}, userId={}", gameId, userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void savePick(GamePickDto pick) {
        String key = RedisKeyBuilder.gamePicks(pick.gameId());
        String hashKey = pick.userId().toString();
        try {
            String json = objectMapper.writeValueAsString(pick);
            redisTemplate.opsForHash().put(key, hashKey, json);
            log.debug("Saved pick: gameId={}, userId={}, algorithmId={}", pick.gameId(), pick.userId(), pick.algorithmId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize pick: " + pick.id(), e);
        }
    }

    @Override
    public List<GamePickDto> getPicks(UUID gameId) {
        String key = RedisKeyBuilder.gamePicks(gameId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.values().stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), GamePickDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize pick from gameId={}", gameId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<GamePickDto> getPicksByUser(UUID gameId, UUID userId) {
        String key = RedisKeyBuilder.gamePicks(gameId);
        String hashKey = userId.toString();
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (value == null) {
            return Collections.emptyList();
        }
        try {
            GamePickDto pick = objectMapper.readValue(value.toString(), GamePickDto.class);
            return List.of(pick);
        } catch (IOException e) {
            log.error("Failed to deserialize pick: gameId={}, userId={}", gameId, userId, e);
            return Collections.emptyList();
        }
    }
}
