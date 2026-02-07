-- V2: users 테이블 및 refresh_tokens 테이블 생성

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kakao_id VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(20) NOT NULL UNIQUE,
    language VARCHAR(20) NOT NULL CHECK (language IN ('JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT')),
    tier VARCHAR(30) NOT NULL DEFAULT 'Iron',
    score INTEGER NOT NULL DEFAULT 0,
    exp INTEGER NOT NULL DEFAULT 0 CHECK (exp >= 0),
    coin INTEGER NOT NULL DEFAULT 0 CHECK (coin >= 0),
    active_game_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_score_desc ON users (score DESC);
CREATE INDEX idx_users_active_game_id ON users (active_game_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
