package com.doan.backend.service;

import com.doan.backend.domain.document.DirectConversationDocument;
import com.doan.backend.domain.document.DirectMessageDocument;
import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.dto.direct.DirectConversationCreateRequest;
import com.doan.backend.dto.direct.DirectConversationResponse;
import com.doan.backend.dto.direct.DirectMessageResponse;
import com.doan.backend.dto.ws.DirectMessageSocketRequest;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.exception.ForbiddenException;
import com.doan.backend.repository.DirectConversationRepository;
import com.doan.backend.repository.DirectMessageRepository;
import com.doan.backend.repository.UserRepository;
import com.doan.backend.support.EntityMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DirectMessageService {
    private final DirectConversationRepository directConversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DirectMessageService(
        DirectConversationRepository directConversationRepository,
        DirectMessageRepository directMessageRepository,
        UserRepository userRepository,
        FileStorageService fileStorageService
    ) {
        this.directConversationRepository = directConversationRepository;
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<DirectConversationResponse> getConversations(String currentUserId) {
        return directConversationRepository.findAllByParticipantIdsContaining(currentUserId)
            .stream()
            .map(EntityMapper::toDirectConversationResponse)
            .toList();
    }

    public DirectConversationResponse createConversation(String currentUserId, DirectConversationCreateRequest request) {
        String targetUserId = request.targetUserId().trim();
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot create direct conversation with yourself");
        }
        ensureUserExists(targetUserId);

        DirectConversationDocument existing = directConversationRepository.findAllByParticipantIdsContaining(currentUserId)
            .stream()
            .filter(conversation -> conversation.getParticipantIds().size() == 2
                && conversation.getParticipantIds().contains(currentUserId)
                && conversation.getParticipantIds().contains(targetUserId))
            .findFirst()
            .orElse(null);

        if (existing != null) {
            return EntityMapper.toDirectConversationResponse(existing);
        }

        DirectConversationDocument saved = directConversationRepository.save(DirectConversationDocument.builder()
            .participantIds(new ArrayList<>(List.of(currentUserId, targetUserId)))
            .createdAt(Instant.now())
            .build());
        return EntityMapper.toDirectConversationResponse(saved);
    }

    public List<DirectMessageResponse> getMessagesByConversation(String currentUserId, String conversationId) {
        DirectConversationDocument conversation = getRequiredConversation(conversationId);
        assertParticipant(conversation, currentUserId);

        List<DirectMessageDocument> messages = new ArrayList<>(
            directMessageRepository.findTop100ByConversationIdOrderByCreatedAtDesc(conversationId)
        );
        Collections.reverse(messages);
        return messages.stream()
            .map(EntityMapper::toDirectMessageResponse)
            .toList();
    }

    public DirectMessageResponse sendDirectMessage(String currentUserId, DirectMessageSocketRequest request) {
        DirectConversationDocument conversation = request.conversationId() == null || request.conversationId().isBlank()
            ? findOrCreateConversation(currentUserId, request.receiverId())
            : getRequiredConversation(request.conversationId());

        assertParticipant(conversation, currentUserId);
        String receiverId = resolveReceiverId(conversation, currentUserId, request.receiverId());

        if ((request.content() == null || request.content().trim().isEmpty())
            && (request.attachmentIds() == null || request.attachmentIds().isEmpty())) {
            throw new BadRequestException("Direct message must contain content or attachments");
        }

        DirectMessageDocument message = DirectMessageDocument.builder()
            .conversationId(conversation.getId())
            .senderId(currentUserId)
            .receiverId(receiverId)
            .content(request.content() == null ? "" : request.content().trim())
            .attachments(fileStorageService.resolveAttachments(request.attachmentIds()))
            .createdAt(Instant.now())
            .deleted(false)
            .build();
        return EntityMapper.toDirectMessageResponse(directMessageRepository.save(message));
    }

    public DirectConversationDocument getRequiredConversation(String conversationId) {
        return directConversationRepository.findById(conversationId)
            .orElseThrow(() -> new BadRequestException("Direct conversation not found"));
    }

    private DirectConversationDocument findOrCreateConversation(String currentUserId, String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new BadRequestException("Receiver id is required when conversation id is not provided");
        }
        return directConversationRepository.findAllByParticipantIdsContaining(currentUserId)
            .stream()
            .filter(conversation -> conversation.getParticipantIds().size() == 2
                && conversation.getParticipantIds().contains(currentUserId)
                && conversation.getParticipantIds().contains(targetUserId))
            .findFirst()
            .orElseGet(() -> {
                ensureUserExists(targetUserId);
                return directConversationRepository.save(DirectConversationDocument.builder()
                    .participantIds(new ArrayList<>(List.of(currentUserId, targetUserId)))
                    .createdAt(Instant.now())
                    .build());
            });
    }

    private void assertParticipant(DirectConversationDocument conversation, String userId) {
        if (!conversation.getParticipantIds().contains(userId)) {
            throw new ForbiddenException("User is not part of this direct conversation");
        }
    }

    private String resolveReceiverId(DirectConversationDocument conversation, String currentUserId, String explicitReceiverId) {
        if (explicitReceiverId != null && !explicitReceiverId.isBlank()) {
            return explicitReceiverId;
        }
        return conversation.getParticipantIds()
            .stream()
            .filter(participantId -> !participantId.equals(currentUserId))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Cannot resolve direct message receiver"));
    }

    private void ensureUserExists(String userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("Target user not found"));
    }
}
