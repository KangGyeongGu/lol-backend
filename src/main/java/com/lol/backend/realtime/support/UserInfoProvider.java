package com.lol.backend.realtime.support;

/**
 * userId → nickname 조회 인터페이스.
 */
public interface UserInfoProvider {

    String getNickname(String userId);
}
