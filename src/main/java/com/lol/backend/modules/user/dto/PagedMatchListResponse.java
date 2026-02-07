package com.lol.backend.modules.user.dto;

import com.lol.backend.common.dto.PageInfo;

import java.util.List;

public record PagedMatchListResponse(
        List<MatchSummaryResponse> items,
        PageInfo page
) {
}
