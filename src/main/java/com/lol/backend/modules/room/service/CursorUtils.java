package com.lol.backend.modules.room.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class CursorUtils {

    private CursorUtils() {}

    public static String encode(Instant updatedAt) {
        if (updatedAt == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(updatedAt.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static Instant decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String iso = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        return Instant.parse(iso);
    }
}
