-- V13: Fix room.room_name length to match OPENAPI spec (VARCHAR(30))
-- OPENAPI.yaml.md CreateRoomRequest: roomName.maxLength: 30

ALTER TABLE room ALTER COLUMN room_name TYPE VARCHAR(30);
