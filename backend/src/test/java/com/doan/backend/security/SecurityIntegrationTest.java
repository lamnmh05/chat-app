package com.doan.backend.security;

import com.doan.backend.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void accessProtectedResource_WithoutToken_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/me"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void accessProtectedResource_WithInvalidToken_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/me")
                        .header("Authorization", "Bearer invalid_fake_token_123"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void accessPublicResource_WithoutToken_ShouldReturn200Ok() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}