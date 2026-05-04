package com.doan.backend.repository;

import com.doan.backend.BaseIntegrationTest;
import com.doan.backend.domain.document.MessageDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

// Kế thừa BaseIntegrationTest và xóa hết các annotation Testcontainers cũ
class MessageRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void findTop100_ShouldReturnExactly100Messages_WhenChannelHasMoreThan100() {
        String channelId = "c_123";
        for (int i = 0; i < 105; i++) {
            messageRepository.save(MessageDocument.builder()
                    .channelId(channelId)
                    .content("Message " + i)
                    .createdAt(Instant.now().plusSeconds(i))
                    .build());
        }

        List<MessageDocument> results = messageRepository.findTop100ByChannelIdOrderByCreatedAtDesc(channelId);

        Assertions.assertEquals(100, results.size());
        Assertions.assertEquals("Message 104", results.getFirst().getContent());
    }
}