package com.lol.backend.common.util;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 컨텍스트에서 현재 사용자 정보를 조회하는 유틸리티.
 */
public class SecurityUtil {

    private SecurityUtil() {
        // 유틸리티 클래스는 인스턴스화 금지
    }

    /**
     * 현재 인증된 사용자의 userId를 반환한다.
     * 인증되지 않은 경우 UNAUTHORIZED 예외를 발생시킨다.
     *
     * @return 현재 사용자 ID
     * @throws BusinessException 인증되지 않은 경우
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인한다.
     *
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
