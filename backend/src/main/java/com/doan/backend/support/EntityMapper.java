package com.doan.backend.support;

import com.doan.backend.domain.document.ChannelDocument;
import com.doan.backend.domain.document.DirectConversationDocument;
import com.doan.backend.domain.document.DirectMessageDocument;
import com.doan.backend.domain.document.GuildDocument;
import com.doan.backend.domain.document.GuildMemberDocument;
import com.doan.backend.domain.document.MessageDocument;
import com.doan.backend.domain.document.StoredFileDocument;
import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.model.Attachment;
import com.doan.backend.domain.model.Reaction;
import com.doan.backend.dto.channel.ChannelResponse;
import com.doan.backend.dto.direct.DirectConversationResponse;
import com.doan.backend.dto.direct.DirectMessageResponse;
import com.doan.backend.dto.file.FileUploadResponse;
import com.doan.backend.dto.guild.GuildMemberResponse;
import com.doan.backend.dto.guild.GuildResponse;
import com.doan.backend.dto.message.AttachmentResponse;
import com.doan.backend.dto.message.MessageResponse;
import com.doan.backend.dto.message.ReactionResponse;
import com.doan.backend.dto.user.UserResponse;
import java.util.Collections;
import java.util.List;

public final class EntityMapper {
    private EntityMapper() {
    }

    public static UserResponse toUserResponse(UserDocument user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public static GuildResponse toGuildResponse(GuildDocument guild) {
        return new GuildResponse(
            guild.getId(),
            guild.getName(),
            guild.getAvatarUrl(),
            guild.getOwnerId(),
            guild.getCreatedAt(),
            guild.getUpdatedAt()
        );
    }

    public static GuildMemberResponse toGuildMemberResponse(
        GuildMemberDocument member,
        UserDocument user,
        List<com.doan.backend.domain.enums.RoleType> roleTypes
    ) {
        return new GuildMemberResponse(
            member.getId(),
            member.getGuildId(),
            member.getUserId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            member.getNickname(),
            member.getRoleIds(),
            roleTypes,
            user.getStatus(),
            member.getJoinedAt()
        );
    }

    public static ChannelResponse toChannelResponse(ChannelDocument channel) {
        return new ChannelResponse(
            channel.getId(),
            channel.getGuildId(),
            channel.getName(),
            channel.getType(),
            channel.getPosition(),
            channel.getCreatedAt(),
            channel.getUpdatedAt()
        );
    }

    public static MessageResponse toMessageResponse(MessageDocument message, UserDocument sender) {
        return new MessageResponse(
            message.getId(),
            message.getChannelId(),
            message.getGuildId(),
            message.getSenderId(),
            sender != null ? sender.getDisplayName() : "Unknown User",
            sender != null ? sender.getAvatarUrl() : null,
            message.getContent(),
            message.getType(),
            message.getReplyToMessageId(),
            mapAttachments(message.getAttachments()),
            mapReactions(message.getReactions()),
            message.getCreatedAt(),
            message.getEditedAt(),
            message.isDeleted()
        );
    }

    public static DirectConversationResponse toDirectConversationResponse(DirectConversationDocument conversation) {
        return new DirectConversationResponse(
            conversation.getId(),
            conversation.getParticipantIds(),
            conversation.getCreatedAt()
        );
    }

    public static DirectMessageResponse toDirectMessageResponse(DirectMessageDocument message) {
        return new DirectMessageResponse(
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
            message.getReceiverId(),
            message.getContent(),
            mapAttachments(message.getAttachments()),
            message.getCreatedAt(),
            message.getEditedAt(),
            message.isDeleted()
        );
    }

    public static FileUploadResponse toFileUploadResponse(StoredFileDocument storedFile) {
        return new FileUploadResponse(
            storedFile.getId(),
            storedFile.getFileName(),
            storedFile.getFileUrl(),
            storedFile.getContentType(),
            storedFile.getSize()
        );
    }

    private static List<AttachmentResponse> mapAttachments(List<Attachment> attachments) {
        if (attachments == null) {
            return Collections.emptyList();
        }
        return attachments.stream()
            .map(EntityMapper::toAttachmentResponse)
            .toList();
    }

    private static List<ReactionResponse> mapReactions(List<Reaction> reactions) {
        if (reactions == null) {
            return Collections.emptyList();
        }
        return reactions.stream()
            .map(reaction -> new ReactionResponse(reaction.getEmoji(), reaction.getUserIds()))
            .toList();
    }

    public static AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return new AttachmentResponse(
            attachment.getId(),
            attachment.getFileName(),
            attachment.getFileUrl(),
            attachment.getContentType(),
            attachment.getSize()
        );
    }
}
