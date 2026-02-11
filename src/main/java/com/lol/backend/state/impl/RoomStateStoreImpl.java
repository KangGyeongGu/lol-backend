package com.lol.backend.state.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.dto.RoomHostHistoryStateDto;
import com.lol.backend.state.dto.RoomKickStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import com.lol.backend.state.store.RoomStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary
public class RoomStateStoreImpl implements RoomStateStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RoomStateStoreImpl(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveRoom(RoomStateDto room) {
        String key = RedisKeyBuilder.room(room.id());
        try {
            String json = objectMapper.writeValueAsString(room);
            redisTemplate.opsForValue().set(key, json, java.time.Duration.ofHours(24));
            log.debug("Saved room state: roomId={}", room.id());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize room state: " + room.id(), e);
        }
    }

    @Override
    public Optional<RoomStateDto> getRoom(UUID roomId) {
        String key = RedisKeyBuilder.room(roomId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            RoomStateDto dto = objectMapper.readValue(json, RoomStateDto.class);
            return Optional.of(dto);
        } catch (IOException e) {
            log.error("Failed to deserialize room state: roomId={}", roomId, e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteRoom(UUID roomId) {
        String roomKey = RedisKeyBuilder.room(roomId);
        String playersKey = RedisKeyBuilder.roomPlayers(roomId);
        String kicksKey = RedisKeyBuilder.roomKicks(roomId);
        String hostHistoryKey = RedisKeyBuilder.roomHostHistory(roomId);
        redisTemplate.delete(roomKey);
        redisTemplate.delete(playersKey);
        redisTemplate.delete(kicksKey);
        redisTemplate.delete(hostHistoryKey);
        log.debug("Deleted room state: roomId={}", roomId);
    }

    @Override
    public List<RoomStateDto> getAllActiveRooms() {
        // Use SCAN instead of KEYS to prevent blocking in production
        org.springframework.data.redis.core.ScanOptions options =
            org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match("room:*")
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

        return keys.stream()
                .filter(key -> !key.contains(":players") && !key.contains(":version")
                        && !key.contains(":kicks") && !key.contains(":host_history"))
                .map(key -> {
                    String json = redisTemplate.opsForValue().get(key);
                    if (json == null) return null;
                    try {
                        return objectMapper.readValue(json, RoomStateDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize room state from key={}", key, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void addPlayer(RoomPlayerStateDto player) {
        String key = RedisKeyBuilder.roomPlayers(player.roomId());
        String hashKey = player.userId().toString();
        try {
            String json = objectMapper.writeValueAsString(player);
            redisTemplate.opsForHash().put(key, hashKey, json);
            log.debug("Added player to room: roomId={}, userId={}", player.roomId(), player.userId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize room player state: " + player.userId(), e);
        }
    }

    /**
     * Atomically add player to room only if not already present (active player with leftAt == null).
     * @return true if player was added, false if player already exists
     */
    public boolean addPlayerIfNotExists(RoomPlayerStateDto player) {
        String key = RedisKeyBuilder.roomPlayers(player.roomId());
        String hashKey = player.userId().toString();

        // Lua script to check if player exists with leftAt == null before adding
        String luaScript =
            "local existing = redis.call('HGET', KEYS[1], ARGV[1]) " +
            "if existing then " +
            "  local json = cjson.decode(existing) " +
            "  if not json.leftAt then " +
            "    return 0 " +
            "  end " +
            "end " +
            "redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]) " +
            "return 1";

        try {
            String json = objectMapper.writeValueAsString(player);
            Long result = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
                    byte[] keyBytes = key.getBytes();
                    byte[] hashKeyBytes = hashKey.getBytes();
                    byte[] jsonBytes = json.getBytes();
                    Object res = connection.eval(
                        luaScript.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        keyBytes,
                        hashKeyBytes,
                        jsonBytes
                    );
                    return res != null ? (Long) res : 0L;
                }
            );

            boolean added = result != null && result == 1L;
            if (added) {
                log.debug("Atomically added player to room: roomId={}, userId={}", player.roomId(), player.userId());
            } else {
                log.debug("Player already exists in room: roomId={}, userId={}", player.roomId(), player.userId());
            }
            return added;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize room player state: " + player.userId(), e);
        }
    }

    @Override
    public void removePlayer(UUID roomId, UUID userId) {
        String key = RedisKeyBuilder.roomPlayers(roomId);
        String hashKey = userId.toString();
        redisTemplate.opsForHash().delete(key, hashKey);
        log.debug("Removed player from room: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public Optional<RoomPlayerStateDto> getPlayer(UUID roomId, UUID userId) {
        String key = RedisKeyBuilder.roomPlayers(roomId);
        String hashKey = userId.toString();
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            RoomPlayerStateDto dto = objectMapper.readValue(value.toString(), RoomPlayerStateDto.class);
            return Optional.of(dto);
        } catch (IOException e) {
            log.error("Failed to deserialize room player state: roomId={}, userId={}", roomId, userId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<RoomPlayerStateDto> getPlayers(UUID roomId) {
        String key = RedisKeyBuilder.roomPlayers(roomId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.values().stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), RoomPlayerStateDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize room player state from roomId={}", roomId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void updatePlayerState(UUID roomId, UUID userId, String state) {
        String key = RedisKeyBuilder.roomPlayers(roomId);
        String hashKey = userId.toString();

        // Atomic read-modify-write using Lua script
        String luaScript =
            "local existing = redis.call('HGET', KEYS[1], ARGV[1]) " +
            "if not existing then return nil end " +
            "local json = cjson.decode(existing) " +
            "json.state = ARGV[2] " +
            "local updated = cjson.encode(json) " +
            "redis.call('HSET', KEYS[1], ARGV[1], updated) " +
            "return updated";

        try {
            String result = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                    byte[] keyBytes = key.getBytes();
                    byte[] hashKeyBytes = hashKey.getBytes();
                    byte[] stateBytes = state.getBytes();
                    Object res = connection.eval(
                        luaScript.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.VALUE,
                        1,
                        keyBytes,
                        hashKeyBytes,
                        stateBytes
                    );
                    return res != null ? new String((byte[]) res) : null;
                }
            );

            if (result == null) {
                log.warn("Cannot update player state: player not found. roomId={}, userId={}", roomId, userId);
            } else {
                log.debug("Updated player state: roomId={}, userId={}, newState={}", roomId, userId, state);
            }
        } catch (Exception e) {
            log.error("Failed to update player state atomically: roomId={}, userId={}", roomId, userId, e);
            throw new RuntimeException("Failed to update player state", e);
        }
    }

    @Override
    public void incrementListVersion() {
        String key = RedisKeyBuilder.roomListVersion();
        redisTemplate.opsForValue().increment(key);
        log.debug("Incremented room list version");
    }

    @Override
    public long getListVersion() {
        String key = RedisKeyBuilder.roomListVersion();
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse list version: {}", value, e);
            return 0L;
        }
    }

    @Override
    public void addKick(RoomKickStateDto kick) {
        String key = RedisKeyBuilder.roomKicks(kick.roomId());
        String hashKey = kick.userId().toString();
        try {
            String json = objectMapper.writeValueAsString(kick);
            redisTemplate.opsForHash().put(key, hashKey, json);
            log.debug("Added kick to room: roomId={}, userId={}", kick.roomId(), kick.userId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize room kick state: " + kick.userId(), e);
        }
    }

    @Override
    public boolean isKicked(UUID roomId, UUID userId) {
        String key = RedisKeyBuilder.roomKicks(roomId);
        String hashKey = userId.toString();
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, hashKey));
    }

    @Override
    public List<RoomKickStateDto> getKicks(UUID roomId) {
        String key = RedisKeyBuilder.roomKicks(roomId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.values().stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), RoomKickStateDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize room kick state from roomId={}", roomId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void addHostHistory(RoomHostHistoryStateDto history) {
        String key = RedisKeyBuilder.roomHostHistory(history.roomId());
        try {
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForList().rightPush(key, json);
            log.debug("Added host history to room: roomId={}, toUserId={}", history.roomId(), history.toUserId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize room host history state", e);
        }
    }

    @Override
    public List<RoomHostHistoryStateDto> getHostHistory(UUID roomId) {
        String key = RedisKeyBuilder.roomHostHistory(roomId);
        List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RoomHostHistoryStateDto.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize room host history state from roomId={}", roomId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
