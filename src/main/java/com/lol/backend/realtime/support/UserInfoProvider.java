package com.lol.backend.realtime.support;

/**
 * userId → nickname 조회 인터페이스.
 * User 모듈이 구현체를 등록하면 stub이 자동 교체된다.
 */
public interface UserInfoProvider {

    String getNickname(String userId);
}
