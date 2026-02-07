-- 초기 스키마 마이그레이션
-- 이 파일은 향후 도메인 모델이 정의되면 실제 테이블 생성 DDL로 채워집니다.
-- 현재는 Flyway 기반 마이그레이션 체계를 확립하기 위한 초기 파일입니다.

-- 마이그레이션 이력 확인용 더미 테이블
CREATE TABLE IF NOT EXISTS migration_info (
    id SERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO migration_info (version, description)
VALUES ('V1', 'Initial migration - infrastructure setup');
