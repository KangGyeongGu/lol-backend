package com.lol.backend.modules.room.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.dto.RoomHostHistoryStateDto;
import com.lol.backend.state.dto.RoomKickStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
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
            redisTemplate.opsForValue().set(key, json);
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
        Set<String> keys = redisTemplate.keys("room:*");
        if (keys == null || keys.isEmpty()) {
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
        Optional<RoomPlayerStateDto> existing = getPlayer(roomId, userId);
        if (existing.isEmpty()) {
            log.warn("Cannot update player state: player not found. roomId={}, userId={}", roomId, userId);
            return;
        }

        RoomPlayerStateDto updated = new RoomPlayerStateDto(
                existing.get().id(),
                roomId,
                userId,
                state,
                existing.get().joinedAt(),
                existing.get().leftAt(),
                existing.get().disconnectedAt()
        );
        addPlayer(updated);
        log.debug("Updated player state: roomId={}, userId={}, newState={}", roomId, userId, state);
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
