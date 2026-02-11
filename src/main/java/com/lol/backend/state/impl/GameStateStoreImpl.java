package com.lol.backend.state.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.store.GameStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary
public class GameStateStoreImpl implements GameStateStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public GameStateStoreImpl(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveGame(GameStateDto game) {
        String key = RedisKeyBuilder.game(game.id());
        try {
            String json = objectMapper.writeValueAsString(game);
            redisTemplate.opsForValue().set(key, json, java.time.Duration.ofHours(4));
            log.debug("Saved game state: gameId={}", game.id());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize game state: " + game.id(), e);
        }
    }

    @Override
    public Optional<GameStateDto> getGame(UUID gameId) {
        String key = RedisKeyBuilder.game(gameId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            GameStateDto dto = objectMapper.readValue(json, GameStateDto.class);
            return Optional.of(dto);
        } catch (IOException e) {
            log.error("Failed to deserialize game state: gameId={}", gameId, e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteGame(UUID gameId) {
        redisTemplate.delete(RedisKeyBuilder.game(gameId));
        redisTemplate.delete(RedisKeyBuilder.gamePlayers(gameId));
        redisTemplate.delete(RedisKeyBuilder.gameBans(gameId));
        redisTemplate.delete(RedisKeyBuilder.gamePicks(gameId));
        redisTemplate.delete(RedisKeyBuilder.gamePurchasesItems(gameId));
        redisTemplate.delete(RedisKeyBuilder.gamePurchasesSpells(gameId));
        redisTemplate.delete(RedisKeyBuilder.effectsActive(gameId));
        log.debug("Deleted game state and all associated keys: gameId={}", gameId);
    }

    @Override
    public void saveGamePlayer(GamePlayerStateDto gamePlayer) {
        String key = RedisKeyBuilder.gamePlayers(gamePlayer.gameId());
        String hashKey = gamePlayer.userId().toString();
        try {
            String json = objectMapper.writeValueAsString(gamePlayer);
            redisTemplate.opsForHash().put(key, hashKey, json);
            log.debug("Saved game player state: gameId={}, userId={}", gamePlayer.gameId(), gamePlayer.userId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize game player state: " + gamePlayer.userId(), e);
        }
    }

    @Override
    public Optional<GamePlayerStateDto> getGamePlayer(UUID gameId, UUID userId) {
        String key = RedisKeyBuilder.gamePlayers(gameId);
        String hashKey = userId.toString();
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            GamePlayerStateDto dto = objectMapper.readValue(value.toString(), GamePlayerStateDto.class);
            return Optional.of(dto);
        } catch (IOException e) {
            log.error("Failed to deserialize game player state: gameId={}, userId={}", gameId, userId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<GamePlayerStateDto> getGamePlayers(UUID gameId) {
        String key = RedisKeyBuilder.gamePlayers(gameId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.values().stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), GamePlayerStateDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize game player state from gameId={}", gameId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void updateGamePlayer(UUID gameId, UUID userId, GamePlayerStateDto updatedPlayer) {
        saveGamePlayer(updatedPlayer);
        log.debug("Updated game player state: gameId={}, userId={}", gameId, userId);
    }

    @Override
    public void updateGameStage(UUID gameId, String stage, Instant stageStartedAt, Instant stageDeadlineAt) {
        String key = RedisKeyBuilder.game(gameId);

        // Atomic read-modify-write using Lua script
        String luaScript =
            "local existing = redis.call('GET', KEYS[1]) " +
            "if not existing then return nil end " +
            "local json = cjson.decode(existing) " +
            "json.stage = ARGV[1] " +
            "json.stageStartedAt = ARGV[2] " +
            "json.stageDeadlineAt = ARGV[3] " +
            "local updated = cjson.encode(json) " +
            "redis.call('SET', KEYS[1], updated, 'KEEPTTL') " +
            "return updated";

        try {
            String stageStartedAtStr = stageStartedAt != null ? stageStartedAt.toString() : "";
            String stageDeadlineAtStr = stageDeadlineAt != null ? stageDeadlineAt.toString() : "";

            String result = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                    byte[] keyBytes = key.getBytes();
                    byte[] stageBytes = stage.getBytes();
                    byte[] startedBytes = stageStartedAtStr.getBytes();
                    byte[] deadlineBytes = stageDeadlineAtStr.getBytes();
                    Object res = connection.eval(
                        luaScript.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.VALUE,
                        1,
                        keyBytes,
                        stageBytes,
                        startedBytes,
                        deadlineBytes
                    );
                    return res != null ? new String((byte[]) res) : null;
                }
            );

            if (result == null) {
                log.warn("Cannot update game stage: game not found. gameId={}", gameId);
            } else {
                log.debug("Updated game stage: gameId={}, newStage={}", gameId, stage);
            }
        } catch (Exception e) {
            log.error("Failed to update game stage atomically: gameId={}", gameId, e);
            throw new RuntimeException("Failed to update game stage", e);
        }
    }

    @Override
    public List<UUID> getAllActiveGameIds() {
        // Use SCAN instead of KEYS to prevent blocking in production
        org.springframework.data.redis.core.ScanOptions options =
            org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match("game:*")
                .count(100)
                .build();

        Set<String> keys = new HashSet<>();
        org.springframework.data.redis.core.Cursor<String> cursor = null;
        try {
            cursor = redisTemplate.scan(options);
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    log.warn("Failed to close cursor", e);
                }
            }
        }

        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        // "game:<uuid>" 형식의 키만 필터링하고 UUID 추출
        return keys.stream()
                .filter(key -> key.matches("^game:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
                .map(key -> UUID.fromString(key.substring(5))) // "game:" 제거
                .collect(Collectors.toList());
    }
}
