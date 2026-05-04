package com.doan.backend.service;

import com.doan.backend.domain.document.ChannelDocument;
import com.doan.backend.domain.document.GuildDocument;
import com.doan.backend.domain.enums.ChannelType;
import com.doan.backend.dto.channel.ChannelCreateRequest;
import com.doan.backend.dto.channel.ChannelResponse;
import com.doan.backend.exception.ForbiddenException;
import com.doan.backend.repository.ChannelRepository;
import com.doan.backend.repository.MessageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock private ChannelRepository channelRepository;
    @Mock private PermissionService permissionService;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private ChannelService channelService;

    @Test
    void createChannel_ShouldCalculatePositionCorrectly() {
        // Arrange
        String currentUserId = "owner_id";
        String guildId = "g1";
        ChannelCreateRequest request = new ChannelCreateRequest("data-engineering", ChannelType.TEXT);

        GuildDocument mockGuild = GuildDocument.builder().id(guildId).build();
        Mockito.when(permissionService.requireGuild(guildId)).thenReturn(mockGuild);
        Mockito.doNothing().when(permissionService).assertCanManageChannels(guildId, currentUserId);

        // Giả sử server đang có 2 kênh, kênh mới tạo phải ở vị trí index = 2
        Mockito.when(channelRepository.countByGuildId(guildId)).thenReturn(2L);
        Mockito.when(channelRepository.save(Mockito.any(ChannelDocument.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ChannelResponse response = channelService.createChannel(currentUserId, guildId, request);

        // Assert
        Assertions.assertEquals("data-engineering", response.name());
        Assertions.assertEquals(2, response.position()); // Kiểm tra logic tính position
    }

    @Test
    void getChannelsByGuild_ShouldThrowForbidden_WhenNotMember() {
        // Arrange
        String currentUserId = "stranger";
        String guildId = "g1";

        Mockito.when(permissionService.requireMembership(guildId, currentUserId))
                .thenThrow(new ForbiddenException("User is not a member of this guild"));

        // Act & Assert
        Assertions.assertThrows(ForbiddenException.class, () -> channelService.getChannelsByGuild(currentUserId, guildId));
        Mockito.verify(channelRepository, Mockito.never()).findAllByGuildIdOrderByPositionAscCreatedAtAsc(Mockito.any());
    }
}