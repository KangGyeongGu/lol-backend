-- V2: Demo data for testing and development
-- 10 users, 7 rooms, 5 games, game_players, bans/picks, purchases, chat messages

-- ============================================================
-- 1. USERS (10명)
-- ============================================================
INSERT INTO users (id, kakao_id, nickname, language, tier, score, exp, coin, active_game_id, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'demo_kakao_1',  'IronCoder',   'JAVA',       'Iron',         100,  150, 500,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 'demo_kakao_2',  'BronzeSnake', 'PYTHON',     'Bronze V',     350,  200, 600,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'demo_kakao_3',  'SilverNinja', 'CPP',        'Silver III',  1000,  300, 700,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444', 'demo_kakao_4',  'GoldScript',  'JAVASCRIPT', 'Gold I',      1700,  400, 800,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555555', 'demo_kakao_5',  'DiamondAce',  'JAVA',       'Diamond II',  2500,  500, 900,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('66666666-6666-6666-6666-666666666666', 'demo_kakao_6',  'MasterMind',  'PYTHON',     'Master',      2900,  600, 1000, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-777777777777', 'demo_kakao_7',  'PlatinumDev', 'JAVA',       'Platinum III',2100,  450, 750,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('88888888-8888-8888-8888-888888888888', 'demo_kakao_8',  'PyBronze',    'PYTHON',     'Bronze II',    500,  180, 400,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('99999999-9999-9999-9999-999999999999', 'demo_kakao_9',  'CppWarrior',  'CPP',        'Gold III',    1500,  350, 650,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', 'demo_kakao_10', 'JsRunner',   'JAVASCRIPT', 'Silver I',    1200,  280, 550,  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 2. ROOMS (7개: 대기2 + 진행1 + 종료4)
-- ============================================================
INSERT INTO room (id, room_name, game_type, language, max_players, host_user_id, created_at, updated_at) VALUES
-- room1: RANKED, JAVA, 진행중 (game2)
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Ranked Battle Room',   'RANKED', 'JAVA',   4, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- room2: NORMAL, PYTHON, 종료 (game1)
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Casual Python Room',  'NORMAL', 'PYTHON', 2, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- room3: 대기 (입장가능, 2/4명)
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Python 초보 환영',      'NORMAL', 'PYTHON', 4, '88888888-8888-8888-8888-888888888888', CURRENT_TIMESTAMP - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '2 minutes'),
-- room4: 대기 (만석, 2/2명)
('ffffffff-ffff-ffff-ffff-ffffffffffff', 'C++ 랭크 듀오',          'RANKED', 'CPP',    2, '99999999-9999-9999-9999-999999999999', CURRENT_TIMESTAMP - INTERVAL '3 minutes', CURRENT_TIMESTAMP - INTERVAL '1 minute'),
-- room5~7: 종료 게임용
('aa111111-aa11-aa11-aa11-aa1111111111', '랭크 대전 #3',           'RANKED', 'JAVA',   2, '77777777-7777-7777-7777-777777777777', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
('aa222222-aa22-aa22-aa22-aa2222222222', '노말 대전 #4',           'NORMAL', 'PYTHON', 2, '88888888-8888-8888-8888-888888888888', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
('aa333333-aa33-aa33-aa33-aa3333333333', '랭크 대전 #5',           'RANKED', 'PYTHON', 2, '66666666-6666-6666-6666-666666666666', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '45 minutes');

-- ============================================================
-- 3. ROOM_PLAYER
-- ============================================================
INSERT INTO room_player (id, room_id, user_id, state, joined_at, left_at, disconnected_at) VALUES
-- room1: user1(READY), user3(READY), user5(UNREADY) — 진행중
(gen_random_uuid(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'READY',   CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL, NULL),
(gen_random_uuid(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'READY',   CURRENT_TIMESTAMP - INTERVAL '9 minutes',  NULL, NULL),
(gen_random_uuid(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-5555-5555-5555-555555555555', 'UNREADY', CURRENT_TIMESTAMP - INTERVAL '8 minutes',  NULL, NULL),
-- room2: user2, user4 — 퇴장 완료
(gen_random_uuid(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'READY', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes', NULL),
(gen_random_uuid(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '44444444-4444-4444-4444-444444444444', 'READY', CURRENT_TIMESTAMP - INTERVAL '28 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes', NULL),
-- room3: 대기 (입장가능)
(gen_random_uuid(), 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '88888888-8888-8888-8888-888888888888', 'READY',   CURRENT_TIMESTAMP - INTERVAL '5 minutes', NULL, NULL),
(gen_random_uuid(), 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', 'UNREADY', CURRENT_TIMESTAMP - INTERVAL '3 minutes', NULL, NULL),
-- room4: 대기 (만석)
(gen_random_uuid(), 'ffffffff-ffff-ffff-ffff-ffffffffffff', '99999999-9999-9999-9999-999999999999', 'READY', CURRENT_TIMESTAMP - INTERVAL '3 minutes', NULL, NULL),
(gen_random_uuid(), 'ffffffff-ffff-ffff-ffff-ffffffffffff', '77777777-7777-7777-7777-777777777777', 'READY', CURRENT_TIMESTAMP - INTERVAL '2 minutes', NULL, NULL),
-- room5~7: 종료 방 참여자 (퇴장 완료)
(gen_random_uuid(), 'aa111111-aa11-aa11-aa11-aa1111111111', '77777777-7777-7777-7777-777777777777', 'READY', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL),
(gen_random_uuid(), 'aa111111-aa11-aa11-aa11-aa1111111111', '99999999-9999-9999-9999-999999999999', 'READY', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL),
(gen_random_uuid(), 'aa222222-aa22-aa22-aa22-aa2222222222', '88888888-8888-8888-8888-888888888888', 'READY', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
(gen_random_uuid(), 'aa222222-aa22-aa22-aa22-aa2222222222', '22222222-2222-2222-2222-222222222222', 'READY', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
(gen_random_uuid(), 'aa333333-aa33-aa33-aa33-aa3333333333', '66666666-6666-6666-6666-666666666666', 'READY', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL),
(gen_random_uuid(), 'aa333333-aa33-aa33-aa33-aa3333333333', '44444444-4444-4444-4444-444444444444', 'READY', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL);

-- ============================================================
-- 4. GAMES (5개: 1 진행중, 4 종료)
-- ============================================================
INSERT INTO game (id, room_id, game_type, stage, stage_started_at, stage_deadline_at, started_at, finished_at, final_algorithm_id, created_at) VALUES
-- game1: room2, NORMAL, FINISHED
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'NORMAL', 'FINISHED',
    CURRENT_TIMESTAMP - INTERVAL '20 minutes', NULL,
    CURRENT_TIMESTAMP - INTERVAL '25 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes',
    (SELECT id FROM algorithm WHERE name = '이진 탐색' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '26 minutes'),
-- game2: room1, RANKED, PLAY 진행중
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'RANKED', 'PLAY',
    CURRENT_TIMESTAMP - INTERVAL '3 minutes', CURRENT_TIMESTAMP + INTERVAL '17 minutes',
    CURRENT_TIMESTAMP - INTERVAL '7 minutes', NULL,
    (SELECT id FROM algorithm WHERE name = '퀵 정렬' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '8 minutes'),
-- game3: room5, RANKED, FINISHED (user7 WIN)
('eeee1111-eeee-eeee-eeee-111111111111', 'aa111111-aa11-aa11-aa11-aa1111111111', 'RANKED', 'FINISHED',
    CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL,
    CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours',
    (SELECT id FROM algorithm WHERE name = '깊이 우선 탐색' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '6 minutes'),
-- game4: room6, NORMAL, FINISHED (user8 WIN)
('eeee2222-eeee-eeee-eeee-222222222222', 'aa222222-aa22-aa22-aa22-aa2222222222', 'NORMAL', 'FINISHED',
    CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL,
    CURRENT_TIMESTAMP - INTERVAL '1 hour' - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '1 hour',
    (SELECT id FROM algorithm WHERE name = '너비 우선 탐색' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '1 hour' - INTERVAL '6 minutes'),
-- game5: room7, RANKED, FINISHED (user6 WIN)
('eeee3333-eeee-eeee-eeee-333333333333', 'aa333333-aa33-aa33-aa33-aa3333333333', 'RANKED', 'FINISHED',
    CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL,
    CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '45 minutes',
    (SELECT id FROM algorithm WHERE name = '다익스트라' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '6 minutes');

-- ============================================================
-- 5. GAME_PLAYER
-- ============================================================
-- game1 (finished): user2(WIN), user4(LOSE)
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at) VALUES
(gen_random_uuid(), 'cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'LEFT', 350, 370, 20, 100, 1, true, 'WIN', 50, 15.0, CURRENT_TIMESTAMP - INTERVAL '26 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL),
(gen_random_uuid(), 'cccccccc-cccc-cccc-cccc-cccccccccccc', '44444444-4444-4444-4444-444444444444', 'LEFT', 1700, 1690, -10, 85, 2, false, 'LOSE', 20, 5.0, CURRENT_TIMESTAMP - INTERVAL '26 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL);

-- game2 (active): user1, user3, user5
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', 'CONNECTED', 100, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL),
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', 'CONNECTED', 1000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL),
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '55555555-5555-5555-5555-555555555555', 'CONNECTED', 2500, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 minutes', NULL, NULL);

-- game3 (finished): user7(WIN), user9(LOSE)
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at) VALUES
(gen_random_uuid(), 'eeee1111-eeee-eeee-eeee-111111111111', '77777777-7777-7777-7777-777777777777', 'LEFT', 2080, 2100, 20, 95, 1, true, 'WIN', 60, 20.0, CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '6 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL),
(gen_random_uuid(), 'eeee1111-eeee-eeee-eeee-111111111111', '99999999-9999-9999-9999-999999999999', 'LEFT', 1520, 1500, -20, 70, 2, false, 'LOSE', 25, 8.0, CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '6 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL);

-- game4 (finished): user8(WIN), user2(LOSE)
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at) VALUES
(gen_random_uuid(), 'eeee2222-eeee-eeee-eeee-222222222222', '88888888-8888-8888-8888-888888888888', 'LEFT', 480, 500, 20, 90, 1, true, 'WIN', 45, 18.0, CURRENT_TIMESTAMP - INTERVAL '1 hour' - INTERVAL '6 minutes', CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
(gen_random_uuid(), 'eeee2222-eeee-eeee-eeee-222222222222', '22222222-2222-2222-2222-222222222222', 'LEFT', 350, 340, -10, 60, 2, false, 'LOSE', 15, 5.0, CURRENT_TIMESTAMP - INTERVAL '1 hour' - INTERVAL '6 minutes', CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL);

-- game5 (finished): user6(WIN), user4(LOSE)
INSERT INTO game_player (id, game_id, user_id, state, score_before, score_after, score_delta, final_score_value, rank_in_game, solved, result, coin_delta, exp_delta, joined_at, left_at, disconnected_at) VALUES
(gen_random_uuid(), 'eeee3333-eeee-eeee-eeee-333333333333', '66666666-6666-6666-6666-666666666666', 'LEFT', 2880, 2900, 20, 100, 1, true, 'WIN', 70, 25.0, CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '6 minutes', CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL),
(gen_random_uuid(), 'eeee3333-eeee-eeee-eeee-333333333333', '44444444-4444-4444-4444-444444444444', 'LEFT', 1690, 1700, 10, 85, 2, true, 'LOSE', 30, 10.0, CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '6 minutes', CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL);

-- ============================================================
-- 6. UPDATE users.active_game_id (user1, user3, user5 → game2)
-- ============================================================
UPDATE users SET active_game_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd', updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333333',
    '55555555-5555-5555-5555-555555555555'
);

-- ============================================================
-- 7. GAME_BAN
-- ============================================================
INSERT INTO game_ban (id, game_id, user_id, algorithm_id, created_at) VALUES
-- game2
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', (SELECT id FROM algorithm WHERE name = '퀵 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '6 minutes'),
-- game3
(gen_random_uuid(), 'eeee1111-eeee-eeee-eeee-111111111111', '77777777-7777-7777-7777-777777777777', (SELECT id FROM algorithm WHERE name = '병합 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '4 minutes'),
(gen_random_uuid(), 'eeee1111-eeee-eeee-eeee-111111111111', '99999999-9999-9999-9999-999999999999', (SELECT id FROM algorithm WHERE name = '힙 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '3 minutes'),
-- game5
(gen_random_uuid(), 'eeee3333-eeee-eeee-eeee-333333333333', '66666666-6666-6666-6666-666666666666', (SELECT id FROM algorithm WHERE name = '퀵 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '4 minutes'),
(gen_random_uuid(), 'eeee3333-eeee-eeee-eeee-333333333333', '44444444-4444-4444-4444-444444444444', (SELECT id FROM algorithm WHERE name = '이진 탐색' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '3 minutes');

-- ============================================================
-- 8. GAME_PICK
-- ============================================================
INSERT INTO game_pick (id, game_id, user_id, algorithm_id, created_at) VALUES
-- game2
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', (SELECT id FROM algorithm WHERE name = '이진 탐색' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
-- game3
(gen_random_uuid(), 'eeee1111-eeee-eeee-eeee-111111111111', '77777777-7777-7777-7777-777777777777', (SELECT id FROM algorithm WHERE name = '깊이 우선 탐색' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '2 minutes'),
(gen_random_uuid(), 'eeee1111-eeee-eeee-eeee-111111111111', '99999999-9999-9999-9999-999999999999', (SELECT id FROM algorithm WHERE name = '버블 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '2 hours' - INTERVAL '1 minute'),
-- game5
(gen_random_uuid(), 'eeee3333-eeee-eeee-eeee-333333333333', '66666666-6666-6666-6666-666666666666', (SELECT id FROM algorithm WHERE name = '다익스트라' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '2 minutes'),
(gen_random_uuid(), 'eeee3333-eeee-eeee-eeee-333333333333', '44444444-4444-4444-4444-444444444444', (SELECT id FROM algorithm WHERE name = '삽입 정렬' LIMIT 1), CURRENT_TIMESTAMP - INTERVAL '45 minutes' - INTERVAL '1 minute');

-- ============================================================
-- 9. GAME_ITEM_PURCHASE (game2)
-- ============================================================
INSERT INTO game_item_purchase (id, game_id, user_id, item_id, quantity, unit_price, total_price, purchased_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', (SELECT id FROM item WHERE name = '해킹' LIMIT 1), 1, 220, 220, CURRENT_TIMESTAMP - INTERVAL '4 minutes');

-- ============================================================
-- 10. GAME_SPELL_PURCHASE (game2)
-- ============================================================
INSERT INTO game_spell_purchase (id, game_id, user_id, spell_id, quantity, unit_price, total_price, purchased_at) VALUES
(gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', (SELECT id FROM spell WHERE name = '보호막' LIMIT 1), 1, 500, 500, CURRENT_TIMESTAMP - INTERVAL '3 minutes');

-- ============================================================
-- 11. CHAT_MESSAGE
-- ============================================================
INSERT INTO chat_message (id, channel_type, room_id, sender_user_id, message, created_at) VALUES
-- room1 (game2) INGAME
(gen_random_uuid(), 'INGAME', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Let''s win this!', CURRENT_TIMESTAMP - INTERVAL '2 minutes'),
(gen_random_uuid(), 'INGAME', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'Good luck everyone!', CURRENT_TIMESTAMP - INTERVAL '1 minute'),
-- room3 GLOBAL
(gen_random_uuid(), 'GLOBAL', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '88888888-8888-8888-8888-888888888888', '파이썬 초보인데 괜찮을까요?', CURRENT_TIMESTAMP - INTERVAL '4 minutes'),
(gen_random_uuid(), 'GLOBAL', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', '저도 초보에요! 같이해요', CURRENT_TIMESTAMP - INTERVAL '3 minutes'),
-- room4 GLOBAL
(gen_random_uuid(), 'GLOBAL', 'ffffffff-ffff-ffff-ffff-ffffffffffff', '99999999-9999-9999-9999-999999999999', '레디 해주세요', CURRENT_TIMESTAMP - INTERVAL '2 minutes'),
(gen_random_uuid(), 'GLOBAL', 'ffffffff-ffff-ffff-ffff-ffffffffffff', '77777777-7777-7777-7777-777777777777', '준비 완료!', CURRENT_TIMESTAMP - INTERVAL '1 minute');
