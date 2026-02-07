package com.lol.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.backend.common.dto.ErrorDetail;
import com.lol.backend.common.dto.ErrorResponse;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.common.util.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패 시 에러 응답을 생성하는 EntryPoint.
 * AUTH_GUARDS.md 2. 인증 필수 규칙 참조.
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPointImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String requestId = RequestContextHolder.getRequestId();

        ErrorDetail errorDetail = ErrorDetail.of(
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.UNAUTHORIZED.getDefaultMessage()
        );

        ErrorResponse errorResponse = ErrorResponse.of(errorDetail, requestId);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
