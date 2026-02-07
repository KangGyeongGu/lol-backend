package com.lol.backend.state;

import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;

import java.util.List;
import java.util.UUID;

public interface BanPickStateStore {

    void saveBan(GameBanDto ban);

    List<GameBanDto> getBans(UUID gameId);

    List<GameBanDto> getBansByUser(UUID gameId, UUID userId);

    void savePick(GamePickDto pick);

    List<GamePickDto> getPicks(UUID gameId);

    List<GamePickDto> getPicksByUser(UUID gameId, UUID userId);
}
