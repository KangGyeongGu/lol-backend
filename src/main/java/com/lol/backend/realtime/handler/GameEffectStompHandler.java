package com.lol.backend.realtime.handler;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.service.GameEffectService;
import com.lol.backend.realtime.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.*;

/**
 * 게임 이펙트(아이템/스펠 사용) STOMP 핸들러.
 * STOMP 메시지 수신 및 비즈니스 로직 위임만 담당.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameEffectStompHandler {

    private final GameEffectService gameEffectService;

    /**
     * ITEM_USE 커맨드 처리.
     * Destination: /app/games/{gameId}/items.use
     */
    @MessageMapping("/games/{gameId}/items.use")
    public void handleItemUse(@DestinationVariable String gameId,
                               CommandEnvelope<ItemUseCommandData> envelope,
                               Principal principal) {
        validatePrincipal(principal);
        UUID userId = parseUUID(principal.getName());
        UUID gameUuid = parseUUID(gameId);
        UUID itemId = parseUUID(envelope.data().itemId());
        UUID targetUserId = parseUUID(envelope.data().targetUserId());

        // 비즈니스 로직은 Service에 위임
        gameEffectService.useItem(gameUuid, userId, itemId, targetUserId);
    }

    /**
     * SPELL_USE 커맨드 처리.
     * Destination: /app/games/{gameId}/spells.use
     */
    @MessageMapping("/games/{gameId}/spells.use")
    public void handleSpellUse(@DestinationVariable String gameId,
                                CommandEnvelope<SpellUseCommandData> envelope,
                                Principal principal) {
        validatePrincipal(principal);
        UUID userId = parseUUID(principal.getName());
        UUID gameUuid = parseUUID(gameId);
        UUID spellId = parseUUID(envelope.data().spellId());

        // 비즈니스 로직은 Service에 위임
        gameEffectService.useSpell(gameUuid, userId, spellId);
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

    private UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "잘못된 UUID 형식입니다: " + value);
        }
    }
}
