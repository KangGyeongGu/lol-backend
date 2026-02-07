package com.lol.backend.modules.chat.controller;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.chat.dto.ChatSendCommandData;
import com.lol.backend.modules.chat.dto.TypingUpdateCommandData;
import com.lol.backend.modules.chat.service.ChatService;
import com.lol.backend.realtime.dto.CommandEnvelope;
import com.lol.backend.realtime.dto.EventEnvelope;
import com.lol.backend.realtime.dto.EventMeta;
import com.lol.backend.realtime.dto.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 채팅/타이핑 STOMP 핸들러.
 */
@Controller
public class ChatStompController {

    private static final Logger log = LoggerFactory.getLogger(ChatStompController.class);

    private final ChatService chatService;

    public ChatStompController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * CHAT_SEND 커맨드 처리.
     * Destination: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleChatSend(CommandEnvelope<ChatSendCommandData> envelope, Principal principal) {
        validatePrincipal(principal);
        chatService.sendChatMessage(principal.getName(), envelope.data());
    }

    /**
     * TYPING_UPDATE 커맨드 처리.
     * Destination: /app/rooms/{roomId}/typing
     */
    @MessageMapping("/rooms/{roomId}/typing")
    public void handleTypingUpdate(@DestinationVariable String roomId,
                                   CommandEnvelope<TypingUpdateCommandData> envelope,
                                   Principal principal) {
        validatePrincipal(principal);
        chatService.updateTypingStatus(principal.getName(), roomId, envelope.data());
    }

    /**
     * BusinessException 핸들러.
     * ERROR 이벤트를 /user/queue/errors로 전송한다.
     */
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public EventEnvelope<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("STOMP BusinessException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());

        Map<String, Object> errorData = Map.of(
                "code", ex.getErrorCode().getCode(),
                "message", ex.getMessage(),
                "details", ex.getDetails() != null ? ex.getDetails() : Map.of()
        );
        return new EventEnvelope<>(EventType.ERROR, errorData, EventMeta.create());
    }

    /**
     * 일반 예외 핸들러.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public EventEnvelope<Map<String, Object>> handleException(Exception ex) {
        log.error("STOMP unexpected error", ex);

        Map<String, Object> errorData = Map.of(
                "code", ErrorCode.INTERNAL_ERROR.getCode(),
                "message", ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                "details", Map.of()
        );
        return new EventEnvelope<>(EventType.ERROR, errorData, EventMeta.create());
    }

    private void validatePrincipal(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
