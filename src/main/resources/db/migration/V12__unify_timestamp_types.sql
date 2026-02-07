-- V12: TIMESTAMP → TIMESTAMP WITH TIME ZONE 통일
-- SSOT 기준: BE_CONVENTIONS.md 5항 (ISO-8601 UTC) + Java Instant 사용
-- V2, V3, V4의 TIMESTAMP 컬럼을 모두 TIMESTAMP WITH TIME ZONE으로 변경

-- ============================================================
-- V1: migration_info
-- ============================================================
ALTER TABLE migration_info ALTER COLUMN applied_at TYPE TIMESTAMP WITH TIME ZONE;

-- ============================================================
-- V2: users, refresh_tokens
-- ============================================================
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE refresh_tokens ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE refresh_tokens ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;

-- ============================================================
-- V3: room domain (room, room_player, room_kick, room_host_history, game, game_player)
-- ============================================================
ALTER TABLE room ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE room ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE room_player ALTER COLUMN joined_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE room_player ALTER COLUMN left_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE room_player ALTER COLUMN disconnected_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE room_kick ALTER COLUMN kicked_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE room_host_history ALTER COLUMN changed_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE game ALTER COLUMN stage_started_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE game ALTER COLUMN stage_deadline_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE game ALTER COLUMN started_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE game ALTER COLUMN finished_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE game ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE game_player ALTER COLUMN joined_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE game_player ALTER COLUMN left_at TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE game_player ALTER COLUMN disconnected_at TYPE TIMESTAMP WITH TIME ZONE;

-- ============================================================
-- V4: chat_message
-- ============================================================
ALTER TABLE chat_message ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;

-- Note: V5, V6는 이미 TIMESTAMP WITH TIME ZONE을 사용하므로 수정 불필요
