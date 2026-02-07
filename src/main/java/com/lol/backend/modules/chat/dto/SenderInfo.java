package com.lol.backend.modules.chat.dto;

/**
 * 채팅 메시지 발신자 정보.
 */
public record SenderInfo(
        String userId,
        String nickname
) {
}
