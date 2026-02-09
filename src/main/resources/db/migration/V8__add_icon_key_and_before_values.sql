-- V8: SSOT 동기화 - icon_key, coin_before, exp_before 추가
-- 2026-02-09: OPENAPI.yaml.md, EVENTS.md 스키마 업데이트 반영

-- 1. ITEM 테이블에 icon_key 추가
ALTER TABLE item ADD COLUMN icon_key VARCHAR(100);

-- 2. SPELL 테이블에 icon_key 추가
ALTER TABLE spell ADD COLUMN icon_key VARCHAR(100);

-- 3. 기존 ITEM 데이터에 icon_key 설정 (클라이언트 아이콘 매핑 키)
UPDATE item SET icon_key = 'hacking'    WHERE name = '해킹';
UPDATE item SET icon_key = 'eclipse'    WHERE name = '월식';
UPDATE item SET icon_key = 'exhaustion' WHERE name = '탈진';
UPDATE item SET icon_key = 'earthquake' WHERE name = '지진';
UPDATE item SET icon_key = 'ignite'     WHERE name = '점화';

-- 4. 기존 SPELL 데이터에 icon_key 설정 (클라이언트 아이콘 매핑 키)
UPDATE spell SET icon_key = 'shield'   WHERE name = '보호막';
UPDATE spell SET icon_key = 'cleanse'  WHERE name = '정화';
UPDATE spell SET icon_key = 'observer' WHERE name = '감시자';

-- 5. GAME_PLAYER 테이블에 coin_before, exp_before 추가
ALTER TABLE game_player ADD COLUMN coin_before INTEGER;
ALTER TABLE game_player ADD COLUMN exp_before DOUBLE PRECISION;

-- migration_info 기록
INSERT INTO migration_info (version, description)
VALUES ('V8', 'Add icon_key to ITEM/SPELL, coin_before/exp_before to GAME_PLAYER');
