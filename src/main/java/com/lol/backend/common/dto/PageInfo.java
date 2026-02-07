package com.lol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 커서 기반 페이징 정보.
 * REST/CONVENTIONS.md 5.2 목록 + 커서 페이징 참조.
 */
public record PageInfo(
        @JsonProperty("limit")
        int limit,

        @JsonProperty("nextCursor")
        String nextCursor
) {
    public static PageInfo of(int limit, String nextCursor) {
        return new PageInfo(limit, nextCursor);
    }
}
