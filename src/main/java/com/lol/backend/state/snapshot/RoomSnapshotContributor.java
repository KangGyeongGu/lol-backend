package com.lol.backend.state.snapshot;

import com.lol.backend.state.dto.RoomStateDto;

import java.util.UUID;

/**
 * Room 도메인의 스냅샷을 DB에 반영하는 책임을 담당한다.
 * state 패키지와 room 모듈 간 의존성을 역전시키기 위한 인터페이스.
 */
public interface RoomSnapshotContributor {

    /**
     * Redis에서 읽은 Room 상태를 DB에 반영한다.
     * Room, RoomPlayer, RoomKick, RoomHostHistory를 모두 처리한다.
     *
     * @param roomState Redis에서 읽은 Room 상태
     */
    void persistRoomSnapshot(RoomStateDto roomState);
}
