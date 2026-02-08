package com.lol.backend.realtime.support;

/**
 * 방/게임 멤버십 확인 인터페이스.
 */
public interface RoomMembershipChecker {

    boolean isMemberOfRoom(String userId, String roomId);
}
