package com.lol.backend.realtime.support;

import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StubRoomMembershipChecker implements RoomMembershipChecker {

    private final RoomStateStore roomStateStore;

    public StubRoomMembershipChecker(RoomStateStore roomStateStore) {
        this.roomStateStore = roomStateStore;
    }

    @Override
    public boolean isMemberOfRoom(String userId, String roomId) {
        Optional<RoomPlayerStateDto> player = roomStateStore.getPlayer(
                UUID.fromString(roomId),
                UUID.fromString(userId)
        );
        return player.isPresent() && player.get().leftAt() == null;
    }
}
