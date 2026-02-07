package com.lol.backend.common.util;

import java.util.UUID;

/**
 * ThreadLocal 기반 요청 컨텍스트 홀더.
 * requestId를 저장/조회한다.
 */
public class RequestContextHolder {
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        String requestId = REQUEST_ID.get();
        if (requestId == null) {
            requestId = generateRequestId();
            REQUEST_ID.set(requestId);
        }
        return requestId;
    }

    public static void clear() {
        REQUEST_ID.remove();
    }

    private static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
