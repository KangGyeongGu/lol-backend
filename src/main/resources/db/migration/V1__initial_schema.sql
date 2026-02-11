-- V1: Initial schema (consolidated)
-- Includes all tables, columns, indexes from V1 + V8 + V9
-- All structural changes merged into initial CREATE TABLE statements

-- ============================================================
-- migration_info
-- ============================================================
CREATE TABLE IF NOT EXISTS migration_info (
    id          SERIAL PRIMARY KEY,
    version     VARCHAR(50) NOT NULL,
    description TEXT,
    applied_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO migration_info (version, description)
VALUES ('V1', 'Initial schema with all tables, columns, and indexes');

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kakao_id       VARCHAR(255) NOT NULL UNIQUE,
    nickname       VARCHAR(20)  NOT NULL UNIQUE,
    language       VARCHAR(20)  NOT NULL CHECK (language IN ('JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT')),
    tier           VARCHAR(30)  NOT NULL DEFAULT 'Iron',
    score          INTEGER      NOT NULL DEFAULT 0,
    exp            DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (exp >= 0),
    coin           INTEGER      NOT NULL DEFAULT 1000 CHECK (coin >= 0),
    active_game_id UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_score_desc ON users (score DESC);
CREATE INDEX idx_users_active_game_id ON users (active_game_id);

-- ============================================================
-- refresh_tokens
-- ============================================================
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ============================================================
-- room
-- ============================================================
CREATE TABLE room (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_name    VARCHAR(30)  NOT NULL,
    game_type    VARCHAR(20)  NOT NULL CHECK (game_type IN ('NORMAL', 'RANKED')),
    language     VARCHAR(20)  NOT NULL CHECK (language IN ('JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT')),
    max_players  INT          NOT NULL CHECK (max_players BETWEEN 2 AND 6),
    host_user_id UUID         NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_room_updated_at ON room (updated_at DESC);
CREATE INDEX idx_room_game_type_language ON room (game_type, language);
CREATE INDEX idx_room_room_name ON room (room_name);
CREATE INDEX idx_room_host_user_id ON room (host_user_id);

-- ============================================================
-- room_player
-- ============================================================
CREATE TABLE room_player (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID        NOT NULL REFERENCES room (id),
    user_id         UUID        NOT NULL REFERENCES users (id),
    state           VARCHAR(20) NOT NULL DEFAULT 'UNREADY' CHECK (state IN ('READY', 'UNREADY', 'DISCONNECTED')),
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at         TIMESTAMPTZ,
    disconnected_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_room_player_active
    ON room_player (room_id, user_id) WHERE left_at IS NULL;

CREATE INDEX idx_room_player_room_active
    ON room_player (room_id) WHERE left_at IS NULL;

-- ============================================================
-- room_kick
-- ============================================================
CREATE TABLE room_kick (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id           UUID        NOT NULL REFERENCES room (id),
    user_id           UUID        NOT NULL REFERENCES users (id),
    kicked_by_user_id UUID        NOT NULL REFERENCES users (id),
    kicked_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_room_kick UNIQUE (room_id, user_id)
);

-- ============================================================
-- room_host_history
-- ============================================================
CREATE TABLE room_host_history (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID        NOT NULL REFERENCES room (id),
    from_user_id UUID        REFERENCES users (id),
    to_user_id   UUID        NOT NULL REFERENCES users (id),
    reason       VARCHAR(20) NOT NULL CHECK (reason IN ('LEAVE', 'SYSTEM', 'MANUAL')),
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- game
-- ============================================================
CREATE TABLE game (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id           UUID        NOT NULL UNIQUE REFERENCES room (id),
    game_type         VARCHAR(20) NOT NULL CHECK (game_type IN ('NORMAL', 'RANKED')),
    stage             VARCHAR(20) NOT NULL DEFAULT 'LOBBY' CHECK (stage IN ('LOBBY', 'BAN', 'PICK', 'SHOP', 'PLAY', 'FINISHED')),
    stage_started_at  TIMESTAMPTZ,
    stage_deadline_at TIMESTAMPTZ,
    started_at        TIMESTAMPTZ,
    finished_at       TIMESTAMPTZ,
    final_algorithm_id UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- game_player
-- ============================================================
CREATE TABLE game_player (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id          UUID        NOT NULL REFERENCES game (id),
    user_id          UUID        NOT NULL REFERENCES users (id),
    state            VARCHAR(20) NOT NULL DEFAULT 'CONNECTED' CHECK (state IN ('CONNECTED', 'DISCONNECTED', 'LEFT')),
    score_before     INT         NOT NULL DEFAULT 0,
    score_after      INT,
    score_delta      INT,
    coin_before      INTEGER,
    exp_before       DOUBLE PRECISION,
    final_score_value INT,
    rank_in_game     INT,
    solved           BOOLEAN,
    result           VARCHAR(10) CHECK (result IN ('WIN', 'LOSE', 'DRAW')),
    coin_delta       INT         CHECK (coin_delta >= 0),
    exp_delta        DOUBLE PRECISION CHECK (exp_delta >= 0),
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at          TIMESTAMPTZ,
    disconnected_at  TIMESTAMPTZ,

    CONSTRAINT uq_game_player UNIQUE (game_id, user_id)
);

-- Composite index for GamePlayerRepository.findByUserIdAndResultIsNotNull()
CREATE INDEX idx_game_player_user_result ON game_player (user_id, result);

-- Composite index for GamePlayerRepository.findByUserIdAndResultIsNotNullAndJoinedAtBeforeOrderByJoinedAtDesc()
CREATE INDEX idx_game_player_user_joined ON game_player (user_id, joined_at DESC);

-- Deferred FK: users.active_game_id → game(id)
ALTER TABLE users
    ADD CONSTRAINT fk_users_active_game FOREIGN KEY (active_game_id) REFERENCES game (id);

-- ============================================================
-- chat_message
-- ============================================================
CREATE TABLE chat_message (
    id             UUID        PRIMARY KEY,
    channel_type   VARCHAR(20) NOT NULL,
    room_id        UUID,
    sender_user_id UUID        NOT NULL,
    message        TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_chat_channel_type CHECK (channel_type IN ('GLOBAL', 'INGAME')),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_chat_message_channel_type ON chat_message (channel_type);
CREATE INDEX idx_chat_message_room_id ON chat_message (room_id);
CREATE INDEX idx_chat_message_created_at ON chat_message (created_at);

-- ============================================================
-- algorithm (catalog)
-- ============================================================
CREATE TABLE algorithm (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- item (catalog)
-- ============================================================
CREATE TABLE item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  TEXT,
    icon_key     VARCHAR(100),
    duration_sec INTEGER      NOT NULL CHECK (duration_sec > 0),
    price        INTEGER      NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- spell (catalog)
-- ============================================================
CREATE TABLE spell (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  TEXT,
    icon_key     VARCHAR(100),
    duration_sec INTEGER      NOT NULL CHECK (duration_sec > 0),
    price        INTEGER      NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- game_ban
-- ============================================================
CREATE TABLE game_ban (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id      UUID NOT NULL,
    user_id      UUID NOT NULL,
    algorithm_id UUID NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (game_id, user_id),
    CONSTRAINT fk_game_ban_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_ban_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_ban_algorithm FOREIGN KEY (algorithm_id) REFERENCES algorithm(id) ON DELETE RESTRICT
);

CREATE INDEX idx_game_ban_game_id ON game_ban (game_id);
CREATE INDEX idx_game_ban_user_id ON game_ban (user_id);

-- ============================================================
-- game_pick
-- ============================================================
CREATE TABLE game_pick (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id      UUID NOT NULL,
    user_id      UUID NOT NULL,
    algorithm_id UUID NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (game_id, user_id),
    CONSTRAINT fk_game_pick_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_pick_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_pick_algorithm FOREIGN KEY (algorithm_id) REFERENCES algorithm(id) ON DELETE RESTRICT
);

CREATE INDEX idx_game_pick_game_id ON game_pick (game_id);
CREATE INDEX idx_game_pick_user_id ON game_pick (user_id);

-- ============================================================
-- game_item_purchase
-- ============================================================
CREATE TABLE game_item_purchase (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id      UUID    NOT NULL,
    user_id      UUID    NOT NULL,
    item_id      UUID    NOT NULL,
    quantity     INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price   INTEGER NOT NULL,
    total_price  INTEGER NOT NULL,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_game_item_purchase_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_item_purchase_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_item_purchase_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE RESTRICT
);

CREATE INDEX idx_game_item_purchase_game_id ON game_item_purchase (game_id);
CREATE INDEX idx_game_item_purchase_user_id ON game_item_purchase (user_id);

-- ============================================================
-- game_spell_purchase
-- ============================================================
CREATE TABLE game_spell_purchase (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id      UUID    NOT NULL,
    user_id      UUID    NOT NULL,
    spell_id     UUID    NOT NULL,
    quantity     INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price   INTEGER NOT NULL,
    total_price  INTEGER NOT NULL,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_game_spell_purchase_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_spell_purchase_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_spell_purchase_spell FOREIGN KEY (spell_id) REFERENCES spell(id) ON DELETE RESTRICT
);

CREATE INDEX idx_game_spell_purchase_game_id ON game_spell_purchase (game_id);
CREATE INDEX idx_game_spell_purchase_user_id ON game_spell_purchase (user_id);

-- ============================================================
-- submission
-- ============================================================
CREATE TABLE submission (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id             UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    language            VARCHAR(20) NOT NULL CHECK (language IN ('JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT')),
    source_code         TEXT        NOT NULL,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_elapsed_ms INTEGER    NOT NULL CHECK (submitted_elapsed_ms >= 0),
    exec_time_ms        INTEGER     NOT NULL CHECK (exec_time_ms >= 0),
    memory_kb           INTEGER     NOT NULL CHECK (memory_kb >= 0),
    judge_status        VARCHAR(10) NOT NULL CHECK (judge_status IN ('AC', 'WA', 'TLE', 'MLE', 'CE', 'RE')),
    judge_detail_json   JSONB,
    score_value         INTEGER,

    CONSTRAINT fk_submission_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_submission_game_id ON submission (game_id);
CREATE INDEX idx_submission_user_id ON submission (user_id);

-- Composite index for SubmissionRepository.findByGameIdAndJudgeStatus()
CREATE INDEX idx_submission_game_status ON submission (game_id, judge_status);

-- ============================================================
-- item_usage
-- ============================================================
CREATE TABLE item_usage (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id      UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id   UUID NOT NULL,
    item_id      UUID NOT NULL,
    used_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_item_usage_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_item_usage_from_user FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_item_usage_to_user FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_item_usage_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE RESTRICT
);

CREATE INDEX idx_item_usage_game_id ON item_usage (game_id);
CREATE INDEX idx_item_usage_from_user_id ON item_usage (from_user_id);

-- ============================================================
-- spell_usage
-- ============================================================
CREATE TABLE spell_usage (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id  UUID NOT NULL,
    user_id  UUID NOT NULL,
    spell_id UUID NOT NULL,
    used_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_spell_usage_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    CONSTRAINT fk_spell_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_spell_usage_spell FOREIGN KEY (spell_id) REFERENCES spell(id) ON DELETE RESTRICT
);

CREATE INDEX idx_spell_usage_game_id ON spell_usage (game_id);
CREATE INDEX idx_spell_usage_user_id ON spell_usage (user_id);

-- ============================================================
-- Deferred FK: game.final_algorithm_id → algorithm(id)
-- ============================================================
ALTER TABLE game ADD CONSTRAINT fk_game_final_algorithm
    FOREIGN KEY (final_algorithm_id) REFERENCES algorithm(id) ON DELETE RESTRICT;

-- ============================================================
-- Catalog seed data
-- ============================================================

-- Algorithms (10)
INSERT INTO algorithm (name, description) VALUES
('버블 정렬', '인접한 두 원소를 비교하며 정렬하는 단순한 알고리즘'),
('선택 정렬', '최솟값을 찾아 앞쪽부터 채우는 정렬 알고리즘'),
('삽입 정렬', '정렬된 부분에 새로운 원소를 삽입하는 정렬 알고리즘'),
('퀵 정렬', '분할 정복 기법을 사용하는 빠른 정렬 알고리즘'),
('병합 정렬', '배열을 반으로 나누고 병합하는 안정 정렬 알고리즘'),
('힙 정렬', '힙 자료구조를 이용한 정렬 알고리즘'),
('이진 탐색', '정렬된 배열에서 값을 빠르게 찾는 탐색 알고리즘'),
('깊이 우선 탐색', '그래프를 깊이 우선으로 순회하는 알고리즘'),
('너비 우선 탐색', '그래프를 너비 우선으로 순회하는 알고리즘'),
('다익스트라', '가중 그래프에서 최단 경로를 찾는 알고리즘');

-- Items (5) with icon_key
INSERT INTO item (name, description, icon_key, duration_sec, price) VALUES
('해킹', '상대방의 물리 키보드를 박살내고, 화상 키보드 코딩을 강제한다.', 'hacking', 10, 220),
('월식', '상대방 화면에 암전 효과를 준다.', 'eclipse', 10, 200),
('탈진', '상대방의 타이핑 속도를 지연시킨다.', 'exhaustion', 10, 200),
('지진', '상대방 화면에 지진 효과를 일으킨다.', 'earthquake', 5, 150),
('점화', '상대방 에디터에 불을 질러 코드를 태운다.', 'ignite', 10, 220);

-- Spells (3) with icon_key
INSERT INTO spell (name, description, icon_key, duration_sec, price) VALUES
('보호막', '5분간 상대방 아이템 효과를 1회 무효화한다.', 'shield', 300, 500),
('정화', '현재 자신에게 적용 중인 모든 아이템 효과를 제거한다.', 'cleanse', 60, 550),
('감시자', '일정 시간 동안 자신의 화면에 상대 상태 인사이트를 표시한다.', 'observer', 60, 350);
