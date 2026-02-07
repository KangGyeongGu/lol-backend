-- V5: 밴/픽/상점 테이블 및 카탈로그 데이터

-- 알고리즘 카탈로그
CREATE TABLE algorithm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 아이템 카탈로그
CREATE TABLE item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    duration_sec INTEGER NOT NULL,
    price INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 스펠 카탈로그
CREATE TABLE spell (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    duration_sec INTEGER NOT NULL,
    price INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 플레이어 밴 선택
CREATE TABLE player_ban (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    algorithm_id UUID NOT NULL REFERENCES algorithm(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(game_id, user_id)
);

CREATE INDEX idx_player_ban_game_id ON player_ban(game_id);
CREATE INDEX idx_player_ban_user_id ON player_ban(user_id);

-- 플레이어 픽 선택
CREATE TABLE player_pick (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    algorithm_id UUID NOT NULL REFERENCES algorithm(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(game_id, user_id)
);

CREATE INDEX idx_player_pick_game_id ON player_pick(game_id);
CREATE INDEX idx_player_pick_user_id ON player_pick(user_id);

-- 플레이어 인벤토리 (아이템)
CREATE TABLE player_inventory_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(game_id, user_id, item_id)
);

CREATE INDEX idx_player_inventory_item_game_user ON player_inventory_item(game_id, user_id);

-- 플레이어 인벤토리 (스펠)
CREATE TABLE player_inventory_spell (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    spell_id UUID NOT NULL REFERENCES spell(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(game_id, user_id, spell_id)
);

CREATE INDEX idx_player_inventory_spell_game_user ON player_inventory_spell(game_id, user_id);

-- game 테이블에 coin 필드 추가 (플레이어당 초기 코인)
ALTER TABLE game ADD COLUMN initial_coin INTEGER NOT NULL DEFAULT 3000;

-- game_player 테이블에 coin 필드 추가 (현재 보유 코인)
ALTER TABLE game_player ADD COLUMN coin INTEGER NOT NULL DEFAULT 3000;

-- 알고리즘 초기 데이터 (예시 10개)
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

-- 아이템 초기 데이터 (CATALOG.md 기준)
INSERT INTO item (name, description, duration_sec, price) VALUES
('해킹', '상대방의 물리 키보드를 박살내고, 화상 키보드 코딩을 강제한다.', 10, 220),
('월식', '상대방 화면에 암전 효과를 준다.', 10, 200),
('탈진', '상대방의 타이핑 속도를 지연시킨다.', 10, 200),
('지진', '상대방 화면에 지진 효과를 일으킨다.', 5, 150),
('점화', '상대방 에디터에 불을 질러 코드를 태운다.', 10, 220);

-- 스펠 초기 데이터 (CATALOG.md 기준)
INSERT INTO spell (name, description, duration_sec, price) VALUES
('보호막', '5분간 상대방 아이템 효과를 1회 무효화한다.', 300, 500),
('정화', '현재 자신에게 적용 중인 모든 아이템 효과를 제거한다.', 60, 550),
('감시자', '일정 시간 동안 자신의 화면에 상대 상태 인사이트를 표시한다.', 60, 350);
