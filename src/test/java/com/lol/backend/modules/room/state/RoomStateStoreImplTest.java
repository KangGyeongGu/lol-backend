package com.lol.backend.modules.room.state;

import com.lol.backend.config.TestcontainersConfig;
import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfig.class)
class RoomStateStoreImplTest {

    @Autowired
    private RoomStateStore roomStateStore;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void cleanup() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    void saveRoom_then_getRoom_success() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        Instant now = Instant.now();

        RoomStateDto room = new RoomStateDto(
                roomId,
                "Test Room",
                "RANKED",
                "PYTHON",
                4,
                hostUserId,
                now,
                now
        );

        // When
        roomStateStore.saveRoom(room);
        Optional<RoomStateDto> retrieved = roomStateStore.getRoom(roomId);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().id()).isEqualTo(roomId);
        assertThat(retrieved.get().roomName()).isEqualTo("Test Room");
        assertThat(retrieved.get().gameType()).isEqualTo("RANKED");
        assertThat(retrieved.get().language()).isEqualTo("PYTHON");
        assertThat(retrieved.get().maxPlayers()).isEqualTo(4);
        assertThat(retrieved.get().hostUserId()).isEqualTo(hostUserId);
    }

    @Test
    void addPlayer_then_getPlayers_success() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Instant now = Instant.now();

        RoomPlayerStateDto player1 = new RoomPlayerStateDto(
                UUID.randomUUID(),
                roomId,
                userId1,
                "READY",
                now,
                null,
                null
        );

        RoomPlayerStateDto player2 = new RoomPlayerStateDto(
                UUID.randomUUID(),
                roomId,
                userId2,
                "UNREADY",
                now,
                null,
                null
        );

        // When
        roomStateStore.addPlayer(player1);
        roomStateStore.addPlayer(player2);
        List<RoomPlayerStateDto> players = roomStateStore.getPlayers(roomId);

        // Then
        assertThat(players).hasSize(2);
        assertThat(players).extracting(RoomPlayerStateDto::userId)
                .containsExactlyInAnyOrder(userId1, userId2);
        assertThat(players).extracting(RoomPlayerStateDto::state)
                .containsExactlyInAnyOrder("READY", "UNREADY");
    }

    @Test
    void removePlayer_success() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        RoomPlayerStateDto player = new RoomPlayerStateDto(
                UUID.randomUUID(),
                roomId,
                userId,
                "READY",
                now,
                null,
                null
        );

        roomStateStore.addPlayer(player);
        assertThat(roomStateStore.getPlayers(roomId)).hasSize(1);

        // When
        roomStateStore.removePlayer(roomId, userId);

        // Then
        assertThat(roomStateStore.getPlayers(roomId)).isEmpty();
    }

    @Test
    void updatePlayerState_success() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        RoomPlayerStateDto player = new RoomPlayerStateDto(
                UUID.randomUUID(),
                roomId,
                userId,
                "UNREADY",
                now,
                null,
                null
        );

        roomStateStore.addPlayer(player);

        // When
        roomStateStore.updatePlayerState(roomId, userId, "READY");

        // Then
        Optional<RoomPlayerStateDto> updated = roomStateStore.getPlayer(roomId, userId);
        assertThat(updated).isPresent();
        assertThat(updated.get().state()).isEqualTo("READY");
    }

    @Test
    void deleteRoom_success() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        Instant now = Instant.now();

        RoomStateDto room = new RoomStateDto(
                roomId,
                "Test Room",
                "RANKED",
                "PYTHON",
                4,
                hostUserId,
                now,
                now
        );

        roomStateStore.saveRoom(room);
        assertThat(roomStateStore.getRoom(roomId)).isPresent();

        // When
        roomStateStore.deleteRoom(roomId);

        // Then
        assertThat(roomStateStore.getRoom(roomId)).isEmpty();
    }

    @Test
    void getAllActiveRooms_success() {
        // Given
        UUID roomId1 = UUID.randomUUID();
        UUID roomId2 = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        Instant now = Instant.now();

        RoomStateDto room1 = new RoomStateDto(
                roomId1,
                "Room 1",
                "RANKED",
                "PYTHON",
                4,
                hostUserId,
                now,
                now
        );

        RoomStateDto room2 = new RoomStateDto(
                roomId2,
                "Room 2",
                "PRACTICE",
                "JAVA",
                2,
                hostUserId,
                now,
                now
        );

        roomStateStore.saveRoom(room1);
        roomStateStore.saveRoom(room2);

        // When
        List<RoomStateDto> activeRooms = roomStateStore.getAllActiveRooms();

        // Then
        assertThat(activeRooms).hasSize(2);
        assertThat(activeRooms).extracting(RoomStateDto::id)
                .containsExactlyInAnyOrder(roomId1, roomId2);
    }

    @Test
    void incrementListVersion_and_getListVersion_success() {
        // Given
        long initialVersion = roomStateStore.getListVersion();

        // When
        roomStateStore.incrementListVersion();
        long version1 = roomStateStore.getListVersion();

        roomStateStore.incrementListVersion();
        long version2 = roomStateStore.getListVersion();

        // Then
        assertThat(version1).isEqualTo(initialVersion + 1);
        assertThat(version2).isEqualTo(initialVersion + 2);
    }

    @Test
    void getPlayer_nonExistent_returnsEmpty() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When
        Optional<RoomPlayerStateDto> player = roomStateStore.getPlayer(roomId, userId);

        // Then
        assertThat(player).isEmpty();
    }

    @Test
    void updatePlayerState_nonExistentPlayer_doesNotThrow() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When & Then (should not throw)
        roomStateStore.updatePlayerState(roomId, userId, "READY");

        // Verify no player was created
        Optional<RoomPlayerStateDto> player = roomStateStore.getPlayer(roomId, userId);
        assertThat(player).isEmpty();
    }
}
