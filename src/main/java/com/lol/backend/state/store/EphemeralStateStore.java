package com.lol.backend.state.store;

import com.lol.backend.state.dto.ConnectionHeartbeatDto;
import com.lol.backend.state.dto.ItemEffectActiveDto;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EphemeralStateStore {

    void saveHeartbeat(ConnectionHeartbeatDto heartbeat, Duration ttl);

    Optional<ConnectionHeartbeatDto> getHeartbeat(UUID userId);

    void saveEffect(ItemEffectActiveDto effect, Duration ttl);

    List<ItemEffectActiveDto> getActiveEffects(UUID gameId);

    void removeEffect(UUID gameId, String uniqueId);
}
