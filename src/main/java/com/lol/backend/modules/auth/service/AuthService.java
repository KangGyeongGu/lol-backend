package com.lol.backend.modules.auth.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.auth.dto.KakaoLoginRequest;
import com.lol.backend.modules.auth.dto.LoginResponse;
import com.lol.backend.modules.auth.dto.SignupRequest;
import com.lol.backend.modules.auth.dto.SignupResponse;
import com.lol.backend.modules.user.dto.UserSummaryResponse;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final KakaoApiClient kakaoApiClient;
    private final AuthTokenProvider authTokenProvider;
    private final UserRepository userRepository;

    public AuthService(KakaoApiClient kakaoApiClient,
                       AuthTokenProvider authTokenProvider,
                       UserRepository userRepository) {
        this.kakaoApiClient = kakaoApiClient;
        this.authTokenProvider = authTokenProvider;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        String kakaoAccessToken = kakaoApiClient.getAccessToken(request.authorizationCode());
        String kakaoId = kakaoApiClient.getUserKakaoId(kakaoAccessToken);

        Optional<User> existingUser = userRepository.findByKakaoId(kakaoId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String accessToken = authTokenProvider.createAccessToken(user.getId());
            String refreshToken = authTokenProvider.createRefreshToken(user.getId());
            return LoginResponse.ok(accessToken, refreshToken, UserSummaryResponse.from(user));
        }

        String signupToken = authTokenProvider.createSignupToken(kakaoId);
        return LoginResponse.signupRequired(signupToken);
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String kakaoId = authTokenProvider.parseSignupToken(request.signupToken());

        if (userRepository.findByKakaoId(kakaoId).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미 가입된 사용자입니다");
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미 사용 중인 닉네임입니다");
        }

        try {
            User user = User.create(kakaoId, request.nickname(), request.language());
            userRepository.save(user);

            String accessToken = authTokenProvider.createAccessToken(user.getId());
            String refreshToken = authTokenProvider.createRefreshToken(user.getId());

            return new SignupResponse(accessToken, refreshToken, UserSummaryResponse.from(user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "닉네임 또는 카카오 계정이 이미 등록되어 있습니다");
        }
    }

    @Transactional
    public void logout(String userId) {
        authTokenProvider.revokeRefreshTokens(UUID.fromString(userId));
    }
}
