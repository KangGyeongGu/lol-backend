package com.lol.backend.modules.chat.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.chat.dto.*;
import com.lol.backend.modules.chat.entity.ChatMessage;
import com.lol.backend.modules.chat.repo.ChatMessageRepository;
import com.lol.backend.realtime.dto.EventType;
import com.lol.backend.realtime.support.EventPublisher;
import com.lol.backend.realtime.support.RoomMembershipChecker;
import com.lol.backend.realtime.support.UserInfoProvider;
import com.lol.backend.state.store.EphemeralStateStore;
import com.lol.backend.state.dto.TypingStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 채팅 전송 / 타이핑 비즈니스 로직.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Duration TYPING_TTL = Duration.ofSeconds(5);

    private final ChatMessageRepository chatMessageRepository;
    private final EventPublisher eventPublisher;
    private final RoomMembershipChecker roomMembershipChecker;
    private final UserInfoProvider userInfoProvider;
    private final EphemeralStateStore ephemeralStateStore;

    /**
     * 채팅 메시지 전송.
     * 검증 → DB 저장 → 이벤트 브로드캐스트.
     */
    @Transactional
    public void sendChatMessage(String userId, ChatSendCommandData commandData) {
        // 검증: message 비어있지 않은지
        if (commandData.message() == null || commandData.message().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "메시지가 비어있습니다");
        }

        // 검증: channelType 필수
        if (commandData.channelType() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "채널 타입이 필요합니다");
        }

        // 검증: INGAME이면 roomId 필수
        UUID roomIdUuid = null;
        if (commandData.channelType() == ChatChannel.INGAME) {
            if (commandData.roomId() == null || commandData.roomId().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "INGAME 채널에서는 roomId가 필수입니다");
            }
            roomIdUuid = UUID.fromString(commandData.roomId());

            // 멤버십 확인
            if (!roomMembershipChecker.isMemberOfRoom(userId, commandData.roomId())) {
                throw new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM);
            }
        }

        // DB 저장
        UUID senderUuid = UUID.fromString(userId);
        ChatMessage chatMessage = new ChatMessage(
                commandData.channelType(),
                roomIdUuid,
                senderUuid,
                commandData.message()
        );
        chatMessageRepository.save(chatMessage);

        // 이벤트 데이터 생성
        String nickname = userInfoProvider.getNickname(userId);
        ChatMessageEventData eventData = new ChatMessageEventData(
                chatMessage.getId().toString(),
                commandData.channelType(),
                commandData.roomId(),
                new SenderInfo(userId, nickname),
                commandData.message(),
                chatMessage.getCreatedAt().toString()
        );

        // 브로드캐스트
        String topic = resolveChatTopic(commandData.channelType(), commandData.roomId());
        eventPublisher.broadcast(topic, EventType.CHAT_MESSAGE, eventData);

        log.debug("Chat message sent: userId={}, channel={}, messageId={}",
                userId, commandData.channelType(), chatMessage.getId());
    }

    /**
     * 타이핑 상태 업데이트.
     * Redis 저장 (ephemeral, TTL 5초) + 이벤트 브로드캐스트.
     */
    public void updateTypingStatus(String userId, String roomId, TypingUpdateCommandData commandData) {
        // 멤버십 확인
        if (!roomMembershipChecker.isMemberOfRoom(userId, roomId)) {
            throw new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM);
        }

        UUID userUuid = UUID.fromString(userId);
        UUID roomUuid = UUID.fromString(roomId);
        Instant now = Instant.now();

        // Redis에 타이핑 상태 저장 (TTL 5초)
        TypingStatusDto typingStatus = new TypingStatusDto(
                userUuid,
                roomUuid,
                commandData.isTyping(),
                now
        );
        ephemeralStateStore.saveTypingStatus(typingStatus, TYPING_TTL);

        // 이벤트 브로드캐스트
        TypingStatusChangedEventData eventData = new TypingStatusChangedEventData(
                roomId,
                userId,
                commandData.isTyping(),
                now.toString()
        );

        String topic = "/topic/rooms/" + roomId + "/typing";
        eventPublisher.broadcast(topic, EventType.TYPING_STATUS_CHANGED, eventData);

        log.debug("Typing status updated and saved to Redis: userId={}, roomId={}, isTyping={}",
                userId, roomId, commandData.isTyping());
    }

    private String resolveChatTopic(ChatChannel channelType, String roomId) {
        return switch (channelType) {
            case GLOBAL -> "/topic/chat/global";
            case INGAME -> "/topic/rooms/" + roomId + "/chat";
        };
    }
}
