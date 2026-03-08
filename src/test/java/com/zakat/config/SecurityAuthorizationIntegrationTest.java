package com.zakat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void viewer_cannotCreatePayment() throws Exception {
        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void operator_cannotMutateZakatQuality() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Standar 2.5 Kg",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "beratPerJiwaKg", 2.5
        );
        mockMvc.perform(post("/api/zakat-qualities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void operator_cannotAccessUserCrud() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void viewer_canAccessReports() throws Exception {
        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/reports/rekap-zis")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString()))
                .andExpect(status().isOk());
    }
}
