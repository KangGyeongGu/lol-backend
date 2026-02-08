package com.lol.backend.state.snapshot;

import java.util.UUID;

/**
 * BanPick 도메인의 스냅샷을 DB에 반영하는 책임을 담당한다.
 * state 패키지와 shop 모듈 간 의존성을 역전시키기 위한 인터페이스.
 */
public interface BanPickSnapshotContributor {

    /**
     * Redis에서 읽은 GameBan/GamePick 데이터를 DB에 반영한다.
     *
     * @param gameId 게임 ID
     */
    void persistBanPickSnapshot(UUID gameId);
}
