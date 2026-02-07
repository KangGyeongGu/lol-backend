-- V3: Create room and game domain tables
-- References existing users table from V2

-- ============================================================
-- 1. ROOM table
-- ============================================================
CREATE TABLE room (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_name       VARCHAR(255) NOT NULL,
    game_type       VARCHAR(20)  NOT NULL CHECK (game_type IN ('NORMAL', 'RANKED')),
    language        VARCHAR(20)  NOT NULL CHECK (language IN ('JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT')),
    max_players     INT          NOT NULL CHECK (max_players BETWEEN 2 AND 6),
    host_user_id    UUID         NOT NULL REFERENCES users (id),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_room_updated_at ON room (updated_at DESC);

-- ============================================================
-- 2. ROOM_PLAYER table
-- ============================================================
CREATE TABLE room_player (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID         NOT NULL REFERENCES room (id),
    user_id         UUID         NOT NULL REFERENCES users (id),
    state           VARCHAR(20)  NOT NULL DEFAULT 'UNREADY' CHECK (state IN ('READY', 'UNREADY', 'DISCONNECTED')),
    joined_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at         TIMESTAMP,
    disconnected_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_room_player_active
    ON room_player (room_id, user_id) WHERE left_at IS NULL;

CREATE INDEX idx_room_player_room_active
    ON room_player (room_id) WHERE left_at IS NULL;

-- ============================================================
-- 3. ROOM_KICK table
-- ============================================================
CREATE TABLE room_kick (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id           UUID      NOT NULL REFERENCES room (id),
    user_id           UUID      NOT NULL REFERENCES users (id),
    kicked_by_user_id UUID      NOT NULL REFERENCES users (id),
    kicked_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_room_kick UNIQUE (room_id, user_id)
);

-- ============================================================
-- 4. ROOM_HOST_HISTORY table
-- ============================================================
CREATE TABLE room_host_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID        NOT NULL REFERENCES room (id),
    from_user_id    UUID        REFERENCES users (id),
    to_user_id      UUID        NOT NULL REFERENCES users (id),
    reason          VARCHAR(20) NOT NULL CHECK (reason IN ('LEAVE', 'SYSTEM', 'MANUAL')),
    changed_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 5. GAME table
-- ============================================================
CREATE TABLE game (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id            UUID        NOT NULL UNIQUE REFERENCES room (id),
    game_type          VARCHAR(20) NOT NULL CHECK (game_type IN ('NORMAL', 'RANKED')),
    stage              VARCHAR(20) NOT NULL DEFAULT 'LOBBY' CHECK (stage IN ('LOBBY', 'BAN', 'PICK', 'SHOP', 'PLAY', 'FINISHED')),
    stage_started_at   TIMESTAMP,
    stage_deadline_at  TIMESTAMP,
    started_at         TIMESTAMP,
    finished_at        TIMESTAMP,
    final_algorithm_id UUID,
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. GAME_PLAYER table
-- ============================================================
CREATE TABLE game_player (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id           UUID        NOT NULL REFERENCES game (id),
    user_id           UUID        NOT NULL REFERENCES users (id),
    state             VARCHAR(20) NOT NULL DEFAULT 'CONNECTED' CHECK (state IN ('CONNECTED', 'DISCONNECTED', 'LEFT')),
    score_before      INT         NOT NULL DEFAULT 0,
    score_after       INT,
    score_delta       INT,
    final_score_value INT,
    rank_in_game      INT,
    solved            BOOLEAN,
    result            VARCHAR(10) CHECK (result IN ('WIN', 'LOSE', 'DRAW')),
    coin_delta        INT         CHECK (coin_delta >= 0),
    exp_delta         DOUBLE PRECISION CHECK (exp_delta >= 0),
    joined_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at           TIMESTAMP,
    disconnected_at   TIMESTAMP,

    CONSTRAINT uq_game_player UNIQUE (game_id, user_id)
);

-- FK for users.active_game_id (deferred because game table must exist first)
ALTER TABLE users
    ADD CONSTRAINT fk_users_active_game FOREIGN KEY (active_game_id) REFERENCES game (id);
