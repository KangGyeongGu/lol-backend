-- V10: FK/인덱스/CHECK 제약조건 보완
-- SSOT 근거: DATA_MODEL.md 7.2, 5.12, 5.6, DB_NOTES.md 1.2

-- ============================================================
-- D3: ITEM/SPELL duration_sec CHECK 제약 추가 (DATA_MODEL.md 7.2)
-- ============================================================
ALTER TABLE item ADD CONSTRAINT chk_item_duration_sec CHECK (duration_sec > 0);
ALTER TABLE spell ADD CONSTRAINT chk_spell_duration_sec CHECK (duration_sec > 0);

-- ============================================================
-- D4: SUBMISSION language/judge_status CHECK 제약 추가 (DATA_MODEL.md 5.12)
-- ============================================================
ALTER TABLE submission ADD CONSTRAINT chk_submission_language
  CHECK (language IN ('JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT'));

ALTER TABLE submission ADD CONSTRAINT chk_submission_judge_status
  CHECK (judge_status IN ('AC', 'WA', 'TLE', 'MLE', 'CE', 'RE'));

-- ============================================================
-- D5: GAME.final_algorithm_id FK 제약 추가 (DATA_MODEL.md 5.6, 7.3)
-- ============================================================
ALTER TABLE game ADD CONSTRAINT fk_game_final_algorithm
  FOREIGN KEY (final_algorithm_id) REFERENCES algorithm(id) ON DELETE RESTRICT;

-- ============================================================
-- D9: CHAT_MESSAGE.sender_user_id FK 제약 추가 (DATA_MODEL.md 5.18, 7.3)
-- ============================================================
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_sender
  FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE RESTRICT;

-- ============================================================
-- D10: ROOM 테이블 인덱스 추가 (DB_NOTES.md 1.2)
-- ============================================================
CREATE INDEX idx_room_game_type_language ON room(game_type, language);
CREATE INDEX idx_room_room_name ON room(room_name);
CREATE INDEX idx_room_host_user_id ON room(host_user_id);
