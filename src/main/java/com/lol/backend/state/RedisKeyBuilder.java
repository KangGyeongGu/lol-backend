package com.lol.backend.state;

import java.util.UUID;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String room(UUID roomId) {
        return "room:" + roomId;
    }

    public static String roomPlayers(UUID roomId) {
        return "room:" + roomId + ":players";
    }

    public static String roomListVersion() {
        return "room:list:version";
    }

    public static String game(UUID gameId) {
        return "game:" + gameId;
    }

    public static String gamePlayers(UUID gameId) {
        return "game:" + gameId + ":players";
    }

    public static String gamePlayer(UUID gameId, UUID userId) {
        return "game:" + gameId + ":players:" + userId;
    }

    public static String gameBans(UUID gameId) {
        return "game:" + gameId + ":bans";
    }

    public static String gamePicks(UUID gameId) {
        return "game:" + gameId + ":picks";
    }

    public static String gamePurchasesItems(UUID gameId) {
        return "game:" + gameId + ":purchases:items";
    }

    public static String gamePurchasesSpells(UUID gameId) {
        return "game:" + gameId + ":purchases:spells";
    }

    public static String typing(UUID roomId, UUID userId) {
        return "typing:" + roomId + ":" + userId;
    }

    public static String heartbeat(UUID userId) {
        return "heartbeat:" + userId;
    }

    public static String effect(UUID gameId, String uniqueId) {
        return "effect:" + gameId + ":" + uniqueId;
    }

    public static String effectsActive(UUID gameId) {
        return "effect:" + gameId + ":active";
    }

    public static String rankingScore() {
        return "ranking:score";
    }
}
