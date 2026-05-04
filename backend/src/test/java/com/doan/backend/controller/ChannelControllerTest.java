package com.doan.backend.controller;

import com.doan.backend.domain.enums.ChannelType;
import com.doan.backend.dto.channel.ChannelCreateRequest;
import com.doan.backend.service.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ChannelControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ChannelService channelService;

    @BeforeEach
    void setup() {
        // Khởi tạo MockMvc thủ công từ Context.
        // Cố tình KHÔNG add Spring Security vào đây để test riêng Validation.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createChannel_ShouldReturn400BadRequest_WhenNameIsNull() throws Exception {
        String jsonPayload = "{\"name\": null, \"type\": \"TEXT\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/guilds/g1/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Validation failed"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.details[0]").value("name: Channel name is required"));

        Mockito.verify(channelService, Mockito.never())
                .createChannel(Mockito.anyString(), Mockito.anyString(), Mockito.any(ChannelCreateRequest.class));
    }
}