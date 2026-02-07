package com.lol.backend.modules.room.dto;

import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.user.entity.Language;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(min = 1, max = 30)
        String roomName,

        @NotNull
        GameType gameType,

        @NotNull
        Language language,

        @NotNull @Min(2) @Max(6)
        Integer maxPlayers
) {}
