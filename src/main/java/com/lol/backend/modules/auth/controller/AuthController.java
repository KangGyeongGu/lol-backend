package com.lol.backend.modules.auth.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.auth.dto.KakaoLoginRequest;
import com.lol.backend.modules.auth.dto.LoginResponse;
import com.lol.backend.modules.auth.dto.SignupRequest;
import com.lol.backend.modules.auth.dto.SignupResponse;
import com.lol.backend.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao/login")
    public ApiResponse<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        LoginResponse response = authService.kakaoLogin(request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        String userId = SecurityUtil.getCurrentUserId();
        authService.logout(userId);
        return ApiResponse.success(null, RequestContextHolder.getRequestId());
    }
}
