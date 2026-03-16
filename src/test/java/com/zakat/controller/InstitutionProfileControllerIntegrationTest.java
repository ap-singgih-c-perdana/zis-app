package com.zakat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.InstitutionProfile;
import com.zakat.repository.InstitutionProfileRepository;
import com.zakat.service.InstitutionProfileService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstitutionProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionProfileRepository institutionProfileRepository;

    @BeforeEach
    void setUp() {
        institutionProfileRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_returns204_whenNotSet() throws Exception {
        mockMvc.perform(get("/api/institution-profile"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void put_upsertsAndGetReturnsIt() throws Exception {
        Map<String, Object> body = Map.of(
                "namaInstansi", "Masjid An Nur",
                "kotaKabupaten", "Lubuklinggau",
                "alamatLengkap", "Jl. Sungkai Rt.01 Ds. Marga Sakti Kec. Muara Kelingi",
                "nomorTelepon", "081234567890",
                "email", "info@masjid.example",
                "namaKetua", "H. Nur Pujianto, S.Kom",
                "namaBendahara", "Ust. Abu Hanifah"
        );

        mockMvc.perform(put("/api/institution-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(InstitutionProfileService.SINGLETON_ID.toString()))
                .andExpect(jsonPath("$.namaInstansi").value("Masjid An Nur"))
                .andExpect(jsonPath("$.kotaKabupaten").value("Lubuklinggau"))
                .andExpect(jsonPath("$.nomorTelepon").value("081234567890"))
                .andExpect(jsonPath("$.email").value("info@masjid.example"));

        mockMvc.perform(get("/api/institution-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(InstitutionProfileService.SINGLETON_ID.toString()))
                .andExpect(jsonPath("$.namaKetua").value("H. Nur Pujianto, S.Kom"))
                .andExpect(jsonPath("$.email").value("info@masjid.example"));

        InstitutionProfile saved = institutionProfileRepository.findById(InstitutionProfileService.SINGLETON_ID).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getNamaInstansi()).isEqualTo("Masjid An Nur");
        org.assertj.core.api.Assertions.assertThat(saved.getNomorTelepon()).isEqualTo("081234567890");
    }
}
