-- 채팅 메시지 테이블
CREATE TABLE chat_message (
    id              UUID            PRIMARY KEY,
    channel_type    VARCHAR(20)     NOT NULL,
    room_id         UUID,
    sender_user_id  UUID            NOT NULL,
    message         TEXT            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_chat_channel_type CHECK (channel_type IN ('GLOBAL', 'INGAME'))
);

CREATE INDEX idx_chat_message_channel_type ON chat_message (channel_type);
CREATE INDEX idx_chat_message_room_id ON chat_message (room_id);
CREATE INDEX idx_chat_message_created_at ON chat_message (created_at);
