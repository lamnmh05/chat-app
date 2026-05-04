package com.doan.backend.controller.ws;

import com.doan.backend.BaseIntegrationTest;
import com.doan.backend.domain.document.*;
import com.doan.backend.domain.enums.ChannelType;
import com.doan.backend.domain.enums.Permission;
import com.doan.backend.domain.enums.RoleType;
import com.doan.backend.repository.*;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class ChatWebSocketIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired private UserRepository userRepository;
    @Autowired private GuildRepository guildRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private GuildMemberRepository guildMemberRepository;
    @Autowired private ChannelRepository channelRepository;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        // ==============================================================
        // VŨ KHÍ BÍ MẬT: Custom Converter bất chấp mọi quy tắc của Spring
        // Không dùng Jackson, không check Type, cứ String <-> Byte mà táng!
        // ==============================================================
        stompClient.setMessageConverter(new MessageConverter() {
            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass) {
                return new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
            }

            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers) {
                return new GenericMessage<>(((String) payload).getBytes(StandardCharsets.UTF_8), headers);
            }
        });

        userRepository.deleteAll();
        guildRepository.deleteAll();
        roleRepository.deleteAll();
        guildMemberRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void shouldReceiveBroadcastMessage_WhenSendingToChatEndpoint() throws Exception {
        UserDocument user = userRepository.save(UserDocument.builder()
                .id("u1").username("testuser").email("test@test.com").passwordHash("hash").build());

        GuildDocument guild = guildRepository.save(GuildDocument.builder()
                .id("g1").name("Test Guild").ownerId(user.getId()).build());

        RoleDocument role = roleRepository.save(RoleDocument.builder()
                .id("r1").guildId(guild.getId()).roleType(RoleType.OWNER)
                .permissions(EnumSet.allOf(Permission.class)).build());

        guildMemberRepository.save(GuildMemberDocument.builder()
                .guildId(guild.getId()).userId(user.getId()).roleIds(List.of(role.getId())).build());

        ChannelDocument channel = channelRepository.save(ChannelDocument.builder()
                .id("c1").guildId(guild.getId()).name("general").type(ChannelType.TEXT).build());

        String wsUrl = "ws://127.0.0.1:" + port + "/ws";

        // Hứng kết quả trực tiếp bằng 1 chuỗi String
        CompletableFuture<String> resultKeeper = new CompletableFuture<>();

        AuthenticatedUser testUser = new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash());
        String validToken = jwtTokenProvider.generateAccessToken(testUser);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + validToken);

        StompSession session = stompClient.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/channel/" + channel.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class; // Yêu cầu trả về đúng String
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                resultKeeper.complete((String) payload);
            }
        });

        Thread.sleep(1000);

        // Gửi tin nhắn bằng JSON
        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/app/chat.sendMessage");
        sendHeaders.setContentType(MimeTypeUtils.APPLICATION_JSON);

        String requestJson = String.format("{\"guildId\":\"%s\",\"channelId\":\"%s\",\"content\":\"Hello Thực Tế!\",\"type\":\"TEXT\",\"attachmentIds\":[]}", guild.getId(), channel.getId());

        // Truyền thẳng String, Custom Converter sẽ tự đổi sang byte[]
        session.send(sendHeaders, requestJson);

        // Đón lấy chuỗi JSON trả về
        String jsonResponse = resultKeeper.get(5, TimeUnit.SECONDS);

        Assertions.assertNotNull(jsonResponse);
        Assertions.assertTrue(jsonResponse.contains("Hello Thực Tế!"));
        Assertions.assertTrue(jsonResponse.contains(user.getId()));
    }
}