package com.lol.backend.modules.room.dto;

import jakarta.validation.constraints.NotBlank;

public record KickRequest(
        @NotBlank
        String targetUserId
) {}
