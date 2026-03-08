package com.zakat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.enums.UserRole;
import com.zakat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminForbiddenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createAdminIsForbidden() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "badadmin",
                "email", "badadmin@yopmail.com",
                "password", "secret",
                "role", UserRole.ADMIN.name()
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateRoleToAdminIsForbidden() throws Exception {
        // create a normal user first
        Map<String, Object> createBody = Map.of(
                "username", "testuser",
                "email", "testuser@yopmail.com",
                "password", "secret",
                "role", UserRole.VIEWER.name()
        );

        String createdJson = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createBody)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extract id
        String id = objectMapper.readTree(createdJson).get("id").asText();

        Map<String, Object> updateBody = Map.of(
                "username", "testuser",
                "email", "testuser@yopmail.com",
                "password", "secret",
                "role", UserRole.ADMIN.name(),
                "active", true
        );

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateBody)))
                .andExpect(status().isForbidden());
    }
}
