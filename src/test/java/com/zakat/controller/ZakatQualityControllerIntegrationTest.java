package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.ZakatQuality;
import com.zakat.enums.ZakatType;
import com.zakat.repository.ZakatQualityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ZakatQualityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZakatQualityRepository zakatQualityRepository;

    @BeforeEach
    void setUp() {
        zakatQualityRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getByType_returnsOnlyMatchingOptions() throws Exception {
        zakatQualityRepository.save(ZakatQuality.builder()
                .name("Standar 2.5 Kg")
                .zakatType(ZakatType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal("2.5"))
                .build());
        zakatQualityRepository.save(ZakatQuality.builder()
                .name("SK Bupati (Standar)")
                .zakatType(ZakatType.ZAKAT_FITRAH_UANG)
                .nominalPerJiwa(45000L)
                .build());
        zakatQualityRepository.save(ZakatQuality.builder()
                .name("Inactive Option")
                .zakatType(ZakatType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal("3.0"))
                .active(false)
                .build());

        String response = mockMvc.perform(get("/api/zakat-qualities")
                        .queryParam("zakatType", "ZAKAT_FITRAH_BERAS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.isArray()).isTrue();
        assertThat(root.size()).isEqualTo(1);
        assertThat(root.get(0).get("zakatType").asText()).isEqualTo("ZAKAT_FITRAH_BERAS");
        assertThat(root.get(0).get("beratPerJiwaKg").asDouble()).isEqualTo(2.5);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void crud_createAndDeactivate_affectsGetByType() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Standar 3.0 Kg",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "beratPerJiwaKg", 3.0
        );

        String createdJson = mockMvc.perform(post("/api/zakat-qualities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        String listBefore = mockMvc.perform(get("/api/zakat-qualities")
                        .queryParam("zakatType", "ZAKAT_FITRAH_BERAS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(listBefore).size()).isEqualTo(1);

        mockMvc.perform(delete("/api/zakat-qualities/{id}", id))
                .andExpect(status().isNoContent());

        String listAfter = mockMvc.perform(get("/api/zakat-qualities")
                        .queryParam("zakatType", "ZAKAT_FITRAH_BERAS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(listAfter).size()).isZero();
    }
}
