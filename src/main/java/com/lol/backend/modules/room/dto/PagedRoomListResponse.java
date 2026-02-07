package com.lol.backend.modules.room.dto;

import com.lol.backend.common.dto.PageInfo;

import java.util.List;

public record PagedRoomListResponse(
        List<RoomSummaryResponse> items,
        PageInfo page,
        long listVersion
) {}
