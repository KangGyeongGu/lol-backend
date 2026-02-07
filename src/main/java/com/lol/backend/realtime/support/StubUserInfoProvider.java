package com.lol.backend.realtime.support;

import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * UserRepository를 사용하여 실제 닉네임을 조회하는 구현체.
 * 사용자를 찾지 못하면 userId를 그대로 반환한다.
 */
@Component
public class StubUserInfoProvider implements UserInfoProvider {

    private final UserRepository userRepository;

    public StubUserInfoProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getNickname(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(User::getNickname)
                .orElse(userId);
    }
}
