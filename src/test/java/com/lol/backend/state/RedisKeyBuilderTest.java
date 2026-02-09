package com.lol.backend.state;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisKeyBuilder 단위 테스트
 * - 각 키 패턴 생성 메서드의 반환값 검증
 * - 일관된 네이밍 규칙 준수 확인
 */
class RedisKeyBuilderTest {

    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GAME_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void room_returnsCorrectKey() {
        String key = RedisKeyBuilder.room(ROOM_ID);
        assertThat(key).isEqualTo("room:11111111-1111-1111-1111-111111111111");
    }

    @Test
    void roomPlayers_returnsCorrectKey() {
        String key = RedisKeyBuilder.roomPlayers(ROOM_ID);
        assertThat(key).isEqualTo("room:11111111-1111-1111-1111-111111111111:players");
    }

    @Test
    void roomListVersion_returnsCorrectKey() {
        String key = RedisKeyBuilder.roomListVersion();
        assertThat(key).isEqualTo("room:list:version");
    }

    @Test
    void game_returnsCorrectKey() {
        String key = RedisKeyBuilder.game(GAME_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222");
    }

    @Test
    void gamePlayers_returnsCorrectKey() {
        String key = RedisKeyBuilder.gamePlayers(GAME_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222:players");
    }

    @Test
    void gamePlayer_returnsCorrectKey() {
        String key = RedisKeyBuilder.gamePlayer(GAME_ID, USER_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222:players:33333333-3333-3333-3333-333333333333");
    }

    @Test
    void gameBans_returnsCorrectKey() {
        String key = RedisKeyBuilder.gameBans(GAME_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222:bans");
    }

    @Test
    void gamePicks_returnsCorrectKey() {
        String key = RedisKeyBuilder.gamePicks(GAME_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222:picks");
    }

    @Test
    void gamePurchasesItems_returnsCorrectKey() {
        String key = RedisKeyBuilder.gamePurchasesItems(GAME_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222:purchases:items");
    }

    @Test
    void gamePurchasesSpells_returnsCorrectKey() {
        String key = RedisKeyBuilder.gamePurchasesSpells(GAME_ID);
        assertThat(key).isEqualTo("game:22222222-2222-2222-2222-222222222222:purchases:spells");
    }

    @Test
    void heartbeat_returnsCorrectKey() {
        String key = RedisKeyBuilder.heartbeat(USER_ID);
        assertThat(key).isEqualTo("heartbeat:33333333-3333-3333-3333-333333333333");
    }

    @Test
    void effect_returnsCorrectKey() {
        String uniqueId = "item-123-spell-456";
        String key = RedisKeyBuilder.effect(GAME_ID, uniqueId);
        assertThat(key).isEqualTo("effect:22222222-2222-2222-2222-222222222222:item-123-spell-456");
    }

    @Test
    void effectsActive_returnsCorrectKey() {
        String key = RedisKeyBuilder.effectsActive(GAME_ID);
        assertThat(key).isEqualTo("effect:22222222-2222-2222-2222-222222222222:active");
    }

    @Test
    void rankingScore_returnsCorrectKey() {
        String key = RedisKeyBuilder.rankingScore();
        assertThat(key).isEqualTo("ranking:score");
    }

    @Test
    void differentRoomIds_produceDifferentKeys() {
        UUID roomId1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID roomId2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        String key1 = RedisKeyBuilder.room(roomId1);
        String key2 = RedisKeyBuilder.room(roomId2);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void sameId_producesConsistentKeys() {
        String key1 = RedisKeyBuilder.game(GAME_ID);
        String key2 = RedisKeyBuilder.game(GAME_ID);

        assertThat(key1).isEqualTo(key2);
    }
}
