-- SSOT DATA_MODEL.md 5.1: exp | double
-- OPENAPI: exp: number, format: double
-- ECONOMY.md: base_exp = 25.0 (소수점 연산 필요)
ALTER TABLE users ALTER COLUMN exp TYPE DOUBLE PRECISION USING exp::DOUBLE PRECISION;
