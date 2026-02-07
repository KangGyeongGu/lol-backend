-- V14: SSOT DATA_MODEL.md에 정의되지 않은 테이블/컬럼 제거

-- 1. player_inventory 테이블 DROP (DATA_MODEL.md에 미정의)
DROP TABLE IF EXISTS player_inventory_item;
DROP TABLE IF EXISTS player_inventory_spell;

-- 2. game.initial_coin 컬럼 DROP (DATA_MODEL.md GAME에 미정의)
ALTER TABLE game DROP COLUMN IF EXISTS initial_coin;

-- 3. game_player.coin 컬럼 DROP (SSOT에는 coin_delta만 정의됨)
ALTER TABLE game_player DROP COLUMN IF EXISTS coin;
