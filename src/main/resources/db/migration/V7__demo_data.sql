-- V7: Demo data for testing and development
-- 6 users, 2 rooms, room players, 1 finished game, 1 active game, purchases, inventory, chat messages

-- ============================================================
-- 1. USERS (6명, 다양한 티어/점수/언어)
-- ============================================================
INSERT INTO users (id, kakao_id, nickname, language, tier, score, exp, coin, active_game_id, created_at, updated_at) VALUES
-- user1: Iron, score=100, JAVA
('11111111-1111-1111-1111-111111111111', 'demo_kakao_1', 'IronCoder', 'JAVA', 'Iron', 100, 150, 500, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- user2: Bronze V, score=350, PYTHON
('22222222-2222-2222-2222-222222222222', 'demo_kakao_2', 'BronzeSnake', 'PYTHON', 'Bronze V', 350, 200, 600, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- user3: Silver III, score=1000, CPP
('33333333-3333-3333-3333-333333333333', 'demo_kakao_3', 'SilverNinja', 'CPP', 'Silver III', 1000, 300, 700, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- user4: Gold I, score=1700, JAVASCRIPT
('44444444-4444-4444-4444-444444444444', 'demo_kakao_4', 'GoldScript', 'JAVASCRIPT', 'Gold I', 1700, 400, 800, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- user5: Diamond II, score=2500, JAVA
('55555555-5555-5555-5555-555555555555', 'demo_kakao_5', 'DiamondAce', 'JAVA', 'Diamond II', 2500, 500, 900, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- user6: Master, score=2900, PYTHON
('66666666-6666-6666-6666-666666666666', 'demo_kakao_6', 'MasterMind', 'PYTHON', 'Master', 2900, 600, 1000, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 2. ROOMS (2개: RANKED + NORMAL)
-- ============================================================
INSERT INTO room (id, room_name, game_type, language, max_players, host_user_id, created_at, updated_at) VALUES
-- room1: RANKED, JAVA, max_players=4, host=user1
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Ranked Battle Room', 'RANKED', 'JAVA', 4, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- room2: NORMAL, PYTHON, max_players=2, host=user2
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Casual Python Room', 'NORMAL', 'PYTHON', 2, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 3. ROOM_PLAYER (방 참여자)
-- ============================================================
INSERT INTO room_player (id, room_id, user_id, state, joined_at, left_at, disconnected_at) VALUES
-- room1: user1(READY), user3(READY), user5(UNREADY)
(gen_random_uuid(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'READY', CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL, NULL),
(gen_random_uuid(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'READY', CURRENT_TIMESTAMP - INTERVAL '9 minutes', NULL, NULL),
(gen_random_uuid(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-5555-5555-5555-555555555555', 'UNREADY', CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL),
-- room2: user2(READY), user4(READY) - but they left after game finished
(gen_random_uuid(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'READY', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes', NULL),
(gen_random_uuid(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '44444444-4444-4444-4444-444444444444', 'READY', CURRENT_TIMESTAMP - INTERVAL '28 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes', NULL);

-- ============================================================
-- 4. GAME (2개: 1 finished, 1 active in PLAY stage)
-- ============================================================
INSERT INTO game (id, room_id, game_type, stage, stage_started_at, stage_deadline_at, started_at, finished_at, final_algorithm_id, created_at, initial_coin) VALUES
-- game1: room2에서 발생, FINISHED
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'NORMAL', 'FINISHED', CURRENT_TIMESTAMP - INTERVAL '20 minutes', NULL, CURRENT_TIMESTAMP - INTERVAL '25 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes', (SELECT id FROM algorithm WHERE name = '이진 탐색' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '26 minutes', 3000),
-- game2: room1에서 발생, PLAY 진행 중
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'RANKED', 'PLAY', CURRENT_TIMESTAMP - INTERVAL '3 minutes', CURRENT_TIMESTAMP + INTERVAL '17 minutes', CURRENT_TIMESTAMP - INTERVAL '7 minutes', NULL, (SELECT id FROM algorithm WHERE name = '퀵 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '8 minutes', 3000);

-- ============================================================
-- 5. GAME_PLAYER
-- ============================================================
-- game1 (finished): user2(WIN), user4(LOSE)
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at, coin) VALUES
(gen_random_uuid(), 'cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'LEFT', 350, 370, 20, 100, 1, true, 'WIN', 50, 15.0, CURRENT_TIMESTAMP - INTERVAL '26 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL, 3200),
(gen_random_uuid(), 'cccccccc-cccc-cccc-cccc-cccccccccccc', '44444444-4444-4444-4444-444444444444', 'LEFT', 1700, 1690, -10, 85, 2, false, 'LOSE', 20, 5.0, CURRENT_TIMESTAMP - INTERVAL '26 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL, 2950);

-- game2 (active): user1, user3, user5
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at, coin) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', 'CONNECTED', 100, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL, 2780),
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', 'CONNECTED', 1000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL, 2450),
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '55555555-5555-5555-5555-555555555555', 'CONNECTED', 2500, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL, 3000);

-- ============================================================
-- 6. UPDATE users.active_game_id (user1, user3, user5 are in game2)
-- ============================================================
UPDATE users SET active_game_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd', updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333333',
    '55555555-5555-5555-5555-555555555555'
);

-- ============================================================
-- 7. GAME_BAN (game2에서 user1이 '퀵 정렬' 밴)
-- ============================================================
INSERT INTO game_ban (id, game_id, user_id, algorithm_id, created_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', (SELECT id FROM algorithm WHERE name = '퀵 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '6 minutes');

-- ============================================================
-- 8. GAME_PICK (game2에서 user1이 '이진 탐색' 픽)
-- ============================================================
INSERT INTO game_pick (id, game_id, user_id, algorithm_id, created_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', (SELECT id FROM algorithm WHERE name = '이진 탐색' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '5 minutes');

-- ============================================================
-- 9. GAME_ITEM_PURCHASE (game2에서 user1이 '해킹' 1개 구매)
-- ============================================================
INSERT INTO game_item_purchase (id, game_id, user_id, item_id, quantity, unit_price, total_price, purchased_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', (SELECT id FROM item WHERE name = '해킹' LIMIT 1), 1, 220, 220, CURRENT_TIMESTAMP - INTERVAL '4 minutes');

-- ============================================================
-- 10. GAME_SPELL_PURCHASE (game2에서 user3가 '보호막' 1개 구매)
-- ============================================================
INSERT INTO game_spell_purchase (id, game_id, user_id, spell_id, quantity, unit_price, total_price, purchased_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', (SELECT id FROM spell WHERE name = '보호막' LIMIT 1), 1, 500, 500, CURRENT_TIMESTAMP - INTERVAL '3 minutes');

-- ============================================================
-- 11. CHAT_MESSAGE (2개)
-- ============================================================
INSERT INTO chat_message (id, channel_type, room_id, sender_user_id, message, created_at) VALUES
-- INGAME message in room1 (game2)
(gen_random_uuid(), 'INGAME', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Let''s win this!', CURRENT_TIMESTAMP - INTERVAL '2 minutes'),
-- INGAME message in room1 (game2)
(gen_random_uuid(), 'INGAME', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'Good luck everyone!', CURRENT_TIMESTAMP - INTERVAL '1 minute');
