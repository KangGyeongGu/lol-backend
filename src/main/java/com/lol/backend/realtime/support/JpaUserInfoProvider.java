package com.lol.backend.realtime.support;

import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * UserRepository(JPA)를 사용하여 닉네임을 조회하는 구현체.
 * 사용자를 찾지 못하면 userId를 그대로 반환한다.
 */
@Component
@RequiredArgsConstructor
public class JpaUserInfoProvider implements UserInfoProvider {

    private final UserRepository userRepository;

    @Override
    public String getNickname(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(User::getNickname)
                .orElse(userId);
    }
}
