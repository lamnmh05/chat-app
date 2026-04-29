package com.doan.backend.service;

import com.doan.backend.domain.document.ChannelDocument;
import com.doan.backend.domain.document.MessageDocument;
import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.enums.MessageType;
import com.doan.backend.domain.model.Reaction;
import com.doan.backend.dto.message.MessageCreateRequest;
import com.doan.backend.dto.message.MessageResponse;
import com.doan.backend.dto.message.MessageUpdateRequest;
import com.doan.backend.dto.message.ReactionCreateRequest;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.exception.NotFoundException;
import com.doan.backend.repository.MessageRepository;
import com.doan.backend.repository.UserRepository;
import com.doan.backend.support.EntityMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChannelService channelService;
    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public MessageService(
        MessageRepository messageRepository,
        ChannelService channelService,
        PermissionService permissionService,
        UserRepository userRepository,
        FileStorageService fileStorageService
    ) {
        this.messageRepository = messageRepository;
        this.channelService = channelService;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public MessageDocument getRequiredMessage(String messageId) {
        return messageRepository.findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    public List<MessageResponse> getMessagesByChannel(String currentUserId, String channelId) {
        ChannelDocument channel = channelService.getRequiredChannel(channelId);
        permissionService.requireMembership(channel.getGuildId(), currentUserId);

        List<MessageDocument> messages = new ArrayList<>(messageRepository.findTop100ByChannelIdOrderByCreatedAtDesc(channelId));
        Collections.reverse(messages);

        Map<String, UserDocument> users = userRepository.findAllById(
                messages.stream().map(MessageDocument::getSenderId).distinct().toList()
            )
            .stream()
            .collect(Collectors.toMap(UserDocument::getId, Function.identity()));

        return messages.stream()
            .map(message -> EntityMapper.toMessageResponse(message, users.get(message.getSenderId())))
            .toList();
    }

    public MessageResponse createMessage(String currentUserId, String channelId, MessageCreateRequest request) {
        ChannelDocument channel = channelService.getRequiredChannel(channelId);
        permissionService.assertCanChat(channel.getGuildId(), currentUserId);
        validateMessagePayload(request.content(), request.type(), request.attachmentIds());

        MessageDocument message = MessageDocument.builder()
            .channelId(channelId)
            .guildId(channel.getGuildId())
            .senderId(currentUserId)
            .content(normalizeContent(request.content()))
            .type(request.type())
            .replyToMessageId(request.replyToMessageId())
            .attachments(fileStorageService.resolveAttachments(request.attachmentIds()))
            .createdAt(Instant.now())
            .deleted(false)
            .build();

        MessageDocument saved = messageRepository.save(message);
        return EntityMapper.toMessageResponse(saved, userRepository.findById(currentUserId).orElse(null));
    }

    public MessageResponse updateMessage(String currentUserId, String messageId, MessageUpdateRequest request) {
        MessageDocument message = getRequiredMessage(messageId);
        permissionService.assertCanEditMessage(message.getGuildId(), currentUserId, message.getSenderId());
        message.setContent(request.content().trim());
        message.setEditedAt(Instant.now());
        MessageDocument saved = messageRepository.save(message);
        return EntityMapper.toMessageResponse(saved, userRepository.findById(saved.getSenderId()).orElse(null));
    }

    public void deleteMessage(String currentUserId, String messageId) {
        MessageDocument message = getRequiredMessage(messageId);
        permissionService.assertCanDeleteMessage(message.getGuildId(), currentUserId, message.getSenderId());
        message.setDeleted(true);
        message.setContent("");
        message.setEditedAt(Instant.now());
        messageRepository.save(message);
    }

    public MessageResponse addReaction(String currentUserId, String messageId, ReactionCreateRequest request) {
        MessageDocument message = getRequiredMessage(messageId);
        permissionService.assertCanReact(message.getGuildId(), currentUserId);
        Reaction reaction = message.getReactions()
            .stream()
            .filter(existing -> existing.getEmoji().equals(request.emoji()))
            .findFirst()
            .orElseGet(() -> {
                Reaction created = Reaction.builder()
                    .emoji(request.emoji())
                    .userIds(new LinkedHashSet<>())
                    .build();
                message.getReactions().add(created);
                return created;
            });

        if (reaction.getUserIds().contains(currentUserId)) {
            reaction.getUserIds().remove(currentUserId);
            if (reaction.getUserIds().isEmpty()) {
                message.getReactions().remove(reaction);
            }
        } else {
            reaction.getUserIds().add(currentUserId);
        }

        MessageDocument saved = messageRepository.save(message);
        return EntityMapper.toMessageResponse(saved, userRepository.findById(saved.getSenderId()).orElse(null));
    }

    private void validateMessagePayload(String content, MessageType type, List<String> attachmentIds) {
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasAttachments = attachmentIds != null && !attachmentIds.isEmpty();
        if (!hasContent && !hasAttachments) {
            throw new BadRequestException("Message must contain content or attachments");
        }
        if ((type == MessageType.FILE || type == MessageType.IMAGE) && !hasAttachments) {
            throw new BadRequestException("File and image messages require attachments");
        }
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim();
    }
}
