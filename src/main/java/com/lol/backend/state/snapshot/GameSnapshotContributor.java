package com.lol.backend.state.snapshot;

import com.lol.backend.state.dto.GameStateDto;

import java.util.UUID;

/**
 * Game 도메인의 스냅샷을 DB에 반영하는 책임을 담당한다.
 * state 패키지와 game/user 모듈 간 의존성을 역전시키기 위한 인터페이스.
 */
public interface GameSnapshotContributor {

    /**
     * Redis에서 읽은 Game 상태를 DB에 반영한다.
     * Game, GamePlayer를 처리하고, 게임 종료 시 User 정산도 수행한다.
     *
     * @param gameState Redis에서 읽은 Game 상태
     */
    void persistGameSnapshot(GameStateDto gameState);
}
