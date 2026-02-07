package com.lol.backend.state;

import java.util.UUID;

public interface SnapshotWriter {

    void persistRoom(UUID roomId);

    void flushRoom(UUID roomId);

    void flushGame(UUID gameId);
}
