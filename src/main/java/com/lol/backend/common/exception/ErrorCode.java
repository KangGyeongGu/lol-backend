package com.lol.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의.
 * ERROR_MODEL.md 4. 에러 코드 참조.
 * 총 22개의 에러 코드가 정의되어 있다.
 */
public enum ErrorCode {
    // 4.1 인증 / 권한 (4개)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    NOT_HOST(HttpStatus.FORBIDDEN, "방장 권한이 필요합니다"),
    KICKED_USER(HttpStatus.FORBIDDEN, "강퇴된 사용자는 재입장할 수 없습니다"),

    // 4.2 전역 상태 (3개)
    ACTIVE_GAME_EXISTS(HttpStatus.CONFLICT, "진행 중인 게임이 있어 새로운 방을 생성하거나 참가할 수 없습니다"),
    INVALID_STAGE_ACTION(HttpStatus.CONFLICT, "현재 게임 단계에서 허용되지 않는 요청입니다"),
    GAME_ALREADY_FINISHED(HttpStatus.CONFLICT, "이미 종료된 게임입니다"),

    // 4.3 Room (4개)
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다"),
    ROOM_FULL(HttpStatus.CONFLICT, "방이 가득 찼습니다"),
    PLAYER_NOT_IN_ROOM(HttpStatus.CONFLICT, "방에 참가하지 않은 플레이어입니다"),
    INVALID_PLAYER_STATE(HttpStatus.CONFLICT, "플레이어 상태가 올바르지 않습니다"),

    // 4.4 Game (1개)
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다"),

    // 4.5 Ban / Pick / Shop (5개)
    DUPLICATED_BAN(HttpStatus.CONFLICT, "이미 밴된 알고리즘입니다"),
    DUPLICATED_PICK(HttpStatus.CONFLICT, "이미 픽된 알고리즘입니다"),
    INSUFFICIENT_COIN(HttpStatus.CONFLICT, "코인이 부족합니다"),
    MAX_ITEM_LIMIT(HttpStatus.CONFLICT, "아이템 최대 보유 개수를 초과했습니다"),
    MAX_SPELL_LIMIT(HttpStatus.CONFLICT, "스펠 최대 보유 개수를 초과했습니다"),

    // 4.6 Submission (2개)
    SUBMISSION_REJECTED(HttpStatus.CONFLICT, "코드 제출이 거부되었습니다"),
    INVALID_LANGUAGE(HttpStatus.BAD_REQUEST, "지원하지 않는 프로그래밍 언어입니다"),

    // 4.7 검증 / 서버 (3개)
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 검증에 실패했습니다"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수 제한을 초과했습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getCode() {
        return this.name();
    }
}
