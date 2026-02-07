package com.lol.backend.realtime.support;

/**
 * 방/게임 멤버십 확인 인터페이스.
 * Room 모듈이 구현체를 등록하면 stub이 자동 교체된다.
 */
public interface RoomMembershipChecker {

    boolean isMemberOfRoom(String userId, String roomId);
}
