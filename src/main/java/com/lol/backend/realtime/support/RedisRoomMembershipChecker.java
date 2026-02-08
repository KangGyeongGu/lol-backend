package com.lol.backend.realtime.support;

import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * RoomStateStore(Redis)를 사용하여 방 멤버십을 확인하는 구현체.
 */
@Component
@RequiredArgsConstructor
public class RedisRoomMembershipChecker implements RoomMembershipChecker {

    private final RoomStateStore roomStateStore;

    @Override
    public boolean isMemberOfRoom(String userId, String roomId) {
        Optional<RoomPlayerStateDto> player = roomStateStore.getPlayer(
                UUID.fromString(roomId),
                UUID.fromString(userId)
        );
        return player.isPresent() && player.get().leftAt() == null;
    }
}
