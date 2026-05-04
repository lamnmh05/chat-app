package com.doan.backend.service;

import com.doan.backend.domain.document.DirectConversationDocument;
import com.doan.backend.dto.direct.DirectConversationCreateRequest;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.repository.DirectConversationRepository;
import com.doan.backend.repository.DirectMessageRepository;
import com.doan.backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock private DirectConversationRepository directConversationRepository;
    @Mock private DirectMessageRepository directMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private DirectMessageService directMessageService;

    @Test
    void createConversation_ShouldThrowBadRequest_WhenTargetIsSelf() {
        // Arrange
        String currentUserId = "nguyen_id";
        DirectConversationCreateRequest request = new DirectConversationCreateRequest("nguyen_id");

        // Act & Assert
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> directMessageService.createConversation(currentUserId, request)
        );
        Assertions.assertEquals("Cannot create direct conversation with yourself", exception.getMessage());
        Mockito.verify(directConversationRepository, Mockito.never()).save(Mockito.any());
    }
}