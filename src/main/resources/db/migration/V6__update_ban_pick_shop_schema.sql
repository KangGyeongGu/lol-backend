-- V6: Ban/Pick/Shop 스키마 명세 준수 업데이트
-- DATA_MODEL.md 기준으로 스키마 수정

-- 1. 테이블명 변경: player_ban → game_ban
ALTER TABLE player_ban RENAME TO game_ban;

-- 2. 테이블명 변경: player_pick → game_pick
ALTER TABLE player_pick RENAME TO game_pick;

-- 3. is_active 필드 추가: algorithm, item, spell
ALTER TABLE algorithm ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE item ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE spell ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- 4. game_item_purchase 테이블 생성 (DATA_MODEL 5.10)
CREATE TABLE game_item_purchase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price INTEGER NOT NULL,
    total_price INTEGER NOT NULL,
    purchased_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_item_purchase_game_id ON game_item_purchase(game_id);
CREATE INDEX idx_game_item_purchase_user_id ON game_item_purchase(user_id);

-- 5. game_spell_purchase 테이블 생성 (DATA_MODEL 5.11)
CREATE TABLE game_spell_purchase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    spell_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price INTEGER NOT NULL,
    total_price INTEGER NOT NULL,
    purchased_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_spell_purchase_game_id ON game_spell_purchase(game_id);
CREATE INDEX idx_game_spell_purchase_user_id ON game_spell_purchase(user_id);

-- 6. submission 테이블 생성 (DATA_MODEL 5.12)
CREATE TABLE submission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    language VARCHAR(20) NOT NULL,
    source_code TEXT NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_elapsed_ms INTEGER NOT NULL CHECK (submitted_elapsed_ms >= 0),
    exec_time_ms INTEGER NOT NULL CHECK (exec_time_ms >= 0),
    memory_kb INTEGER NOT NULL CHECK (memory_kb >= 0),
    judge_status VARCHAR(10) NOT NULL,
    judge_detail_json JSONB,
    score_value INTEGER
);

CREATE INDEX idx_submission_game_id ON submission(game_id);
CREATE INDEX idx_submission_user_id ON submission(user_id);

-- 7. item_usage 테이블 생성 (DATA_MODEL 5.13)
CREATE TABLE item_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    item_id UUID NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_item_usage_game_id ON item_usage(game_id);
CREATE INDEX idx_item_usage_from_user_id ON item_usage(from_user_id);

-- 8. spell_usage 테이블 생성 (DATA_MODEL 5.14)
CREATE TABLE spell_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    spell_id UUID NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_spell_usage_game_id ON spell_usage(game_id);
CREATE INDEX idx_spell_usage_user_id ON spell_usage(user_id);

-- 9. FK 제약 추가 (DATA_MODEL 7.3: 삭제 금지, RESTRICT 사용)
-- game_ban FK
ALTER TABLE game_ban
    ADD CONSTRAINT fk_game_ban_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_ban_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_ban_algorithm FOREIGN KEY (algorithm_id) REFERENCES algorithm(id) ON DELETE RESTRICT;

-- game_pick FK
ALTER TABLE game_pick
    ADD CONSTRAINT fk_game_pick_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_pick_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_pick_algorithm FOREIGN KEY (algorithm_id) REFERENCES algorithm(id) ON DELETE RESTRICT;

-- game_item_purchase FK
ALTER TABLE game_item_purchase
    ADD CONSTRAINT fk_game_item_purchase_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_item_purchase_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_item_purchase_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE RESTRICT;

-- game_spell_purchase FK
ALTER TABLE game_spell_purchase
    ADD CONSTRAINT fk_game_spell_purchase_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_spell_purchase_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_game_spell_purchase_spell FOREIGN KEY (spell_id) REFERENCES spell(id) ON DELETE RESTRICT;

-- submission FK
ALTER TABLE submission
    ADD CONSTRAINT fk_submission_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

-- item_usage FK
ALTER TABLE item_usage
    ADD CONSTRAINT fk_item_usage_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_item_usage_from_user FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_item_usage_to_user FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_item_usage_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE RESTRICT;

-- spell_usage FK
ALTER TABLE spell_usage
    ADD CONSTRAINT fk_spell_usage_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_spell_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_spell_usage_spell FOREIGN KEY (spell_id) REFERENCES spell(id) ON DELETE RESTRICT;

-- 10. 기존 player_ban/player_pick의 CASCADE FK 제거 (이미 테이블명 변경으로 새로 생성됨)
-- 테이블 리네임 시 FK가 유지되므로, 기존 CASCADE FK를 DROP하고 위의 RESTRICT FK로 대체
-- 참고: V5에서 생성된 FK는 자동으로 제거되지 않으므로 명시적 DROP 필요
ALTER TABLE game_ban DROP CONSTRAINT IF EXISTS player_ban_game_id_fkey;
ALTER TABLE game_ban DROP CONSTRAINT IF EXISTS player_ban_user_id_fkey;
ALTER TABLE game_ban DROP CONSTRAINT IF EXISTS player_ban_algorithm_id_fkey;

ALTER TABLE game_pick DROP CONSTRAINT IF EXISTS player_pick_game_id_fkey;
ALTER TABLE game_pick DROP CONSTRAINT IF EXISTS player_pick_user_id_fkey;
ALTER TABLE game_pick DROP CONSTRAINT IF EXISTS player_pick_algorithm_id_fkey;

-- Note: PlayerInventoryItem, PlayerInventorySpell은 DATA_MODEL에 없지만 ephemeral로 유지 (주석 처리)
-- 향후 필요 시 삭제 또는 승격 가능
