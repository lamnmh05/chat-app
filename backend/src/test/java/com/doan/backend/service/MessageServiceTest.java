package com.doan.backend.service;

import com.doan.backend.domain.document.ChannelDocument;
import com.doan.backend.domain.document.MessageDocument;
import com.doan.backend.domain.enums.MessageType;
import com.doan.backend.domain.model.Reaction;
import com.doan.backend.dto.message.MessageCreateRequest;
import com.doan.backend.dto.message.ReactionCreateRequest;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.exception.ForbiddenException;
import com.doan.backend.repository.MessageRepository;
import com.doan.backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ChannelService channelService;
    @Mock private PermissionService permissionService;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void createMessage_ShouldThrowBadRequest_WhenContentAndAttachmentsAreEmpty() {
        // Arrange
        String currentUserId = "u1";
        String channelId = "c1";
        MessageCreateRequest request = new MessageCreateRequest("", MessageType.TEXT, null, new ArrayList<>());

        ChannelDocument mockChannel = ChannelDocument.builder().id(channelId).guildId("g1").build();
        Mockito.when(channelService.getRequiredChannel(channelId)).thenReturn(mockChannel);
        Mockito.doNothing().when(permissionService).assertCanChat("g1", currentUserId);

        // Act & Assert
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> messageService.createMessage(currentUserId, channelId, request)
        );
        Assertions.assertEquals("Message must contain content or attachments", exception.getMessage());
    }

    @Test
    void addReaction_ShouldToggleReaction_WhenCalledTwice() {
        // Arrange
        String currentUserId = "u1";
        String messageId = "m1";
        ReactionCreateRequest request = new ReactionCreateRequest("👍");

        // Giả lập 1 tin nhắn đã có người khác thả tim, nhưng chưa có 👍
        MessageDocument mockMessage = MessageDocument.builder()
                .id(messageId)
                .guildId("g1")
                .reactions(new ArrayList<>(List.of(
                        Reaction.builder().emoji("❤️").userIds(new LinkedHashSet<>(List.of("u2"))).build()
                )))
                .build();

        Mockito.when(messageRepository.findById(messageId)).thenReturn(Optional.of(mockMessage));
        Mockito.doNothing().when(permissionService).assertCanReact("g1", currentUserId);
        Mockito.when(messageRepository.save(Mockito.any(MessageDocument.class))).thenAnswer(i -> i.getArgument(0));

        // Act 1: Thả 👍 lần đầu
        messageService.addReaction(currentUserId, messageId, request);

        // Assert 1: Phải có 2 reaction (❤️ và 👍), 👍 phải chứa "u1"
        Assertions.assertEquals(2, mockMessage.getReactions().size());
        Assertions.assertTrue(mockMessage.getReactions().stream()
                .anyMatch(r -> r.getEmoji().equals("👍") && r.getUserIds().contains("u1")));

        // Act 2: Thả 👍 lần thứ hai (Hủy thả)
        messageService.addReaction(currentUserId, messageId, request);

        // Assert 2: Chỉ còn lại 1 reaction (❤️), 👍 đã bị xóa vì mảng userIds rỗng
        Assertions.assertEquals(1, mockMessage.getReactions().size());
        Assertions.assertEquals("❤️", mockMessage.getReactions().get(0).getEmoji());
    }

    @Test
    void deleteMessage_ShouldThrowForbidden_WhenUserLacksPermission() {
        // Arrange
        String currentUserId = "u_hacker";
        String messageId = "m1";

        MessageDocument mockMessage = MessageDocument.builder()
                .id(messageId)
                .guildId("g1")
                .senderId("u_victim")
                .build();

        Mockito.when(messageRepository.findById(messageId)).thenReturn(Optional.of(mockMessage));
        Mockito.doThrow(new ForbiddenException("User cannot delete this message"))
                .when(permissionService).assertCanDeleteMessage("g1", currentUserId, "u_victim");

        // Act & Assert
        Assertions.assertThrows(ForbiddenException.class, () -> messageService.deleteMessage(currentUserId, messageId));
        Mockito.verify(messageRepository, Mockito.never()).save(Mockito.any());
    }
}