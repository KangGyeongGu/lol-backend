package com.lol.backend.modules.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.common.filter.JwtAuthenticationFilter;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.config.AuthenticationEntryPointImpl;
import com.lol.backend.config.SecurityConfig;
import com.lol.backend.modules.auth.dto.KakaoLoginRequest;
import com.lol.backend.modules.auth.dto.LoginResponse;
import com.lol.backend.modules.auth.dto.SignupRequest;
import com.lol.backend.modules.auth.dto.SignupResponse;
import com.lol.backend.modules.auth.service.AuthService;
import com.lol.backend.modules.user.dto.UserSummaryResponse;
import com.lol.backend.modules.user.entity.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @Test
    void kakaoLogin_success_existingUser() throws Exception {
        KakaoLoginRequest request = new KakaoLoginRequest("valid-auth-code");
        UserSummaryResponse userSummary = new UserSummaryResponse(USER_ID, "testUser", "Gold II", 1500);
        LoginResponse response = LoginResponse.ok("access-token-123", "refresh-token-456", userSummary);

        when(authService.kakaoLogin(any(KakaoLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.result").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.data.user.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.user.nickname").value("testUser"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void kakaoLogin_success_signupRequired() throws Exception {
        KakaoLoginRequest request = new KakaoLoginRequest("new-user-auth-code");
        LoginResponse response = LoginResponse.signupRequired("signup-token-abc");

        when(authService.kakaoLogin(any(KakaoLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.result").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.data.signupToken").value("signup-token-abc"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void signup_success() throws Exception {
        SignupRequest request = new SignupRequest("signup-token-abc", "newPlayer", Language.PYTHON);
        UserSummaryResponse userSummary = new UserSummaryResponse(USER_ID, "newPlayer", "Iron IV", 0);
        SignupResponse response = new SignupResponse("access-token-new", "refresh-token-new", userSummary);

        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.accessToken").value("access-token-new"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-new"))
                .andExpect(jsonPath("$.data.user.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.user.nickname").value("newPlayer"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void logout_success() throws Exception {
        // SecurityUtil.getCurrentUserId() reads authentication.getPrincipal() and expects a String.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(USER_ID, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").exists());

        verify(authService).logout(USER_ID);
    }
}
