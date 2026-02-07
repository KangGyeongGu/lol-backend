package com.lol.backend.modules.chat.entity;

import com.lol.backend.modules.chat.dto.ChatChannel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 채팅 메시지 엔티티.
 * DATA_MODEL.md 5.18 CHAT_MESSAGE 기준.
 * FK 없이 UUID 컬럼만 저장한다 (User/Room 엔티티 부재).
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChatChannel channelType;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(ChatChannel channelType, UUID roomId, UUID senderUserId, String message) {
        this.id = UUID.randomUUID();
        this.channelType = channelType;
        this.roomId = roomId;
        this.senderUserId = senderUserId;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ChatChannel getChannelType() {
        return channelType;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getSenderUserId() {
        return senderUserId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
