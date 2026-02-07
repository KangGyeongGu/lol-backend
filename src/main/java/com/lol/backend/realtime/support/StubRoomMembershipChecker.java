package com.lol.backend.realtime.support;

import com.lol.backend.modules.room.repo.RoomPlayerRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RoomPlayerRepository를 사용하여 실제 멤버십을 확인하는 구현체.
 * 활성 RoomPlayer가 존재하면 멤버로 판단한다.
 */
@Component
public class StubRoomMembershipChecker implements RoomMembershipChecker {

    private final RoomPlayerRepository roomPlayerRepository;

    public StubRoomMembershipChecker(RoomPlayerRepository roomPlayerRepository) {
        this.roomPlayerRepository = roomPlayerRepository;
    }

    @Override
    public boolean isMemberOfRoom(String userId, String roomId) {
        return roomPlayerRepository.findActivePlayer(
                UUID.fromString(roomId),
                UUID.fromString(userId)
        ).isPresent();
    }
}
