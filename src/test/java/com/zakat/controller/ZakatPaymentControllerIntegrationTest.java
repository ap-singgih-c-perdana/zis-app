package com.zakat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.ZakatPayment;
import com.zakat.entity.ZakatQuality;
import com.zakat.enums.ZisType;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.repository.ZakatQualityRepository;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ZakatPaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZakatQualityRepository zakatQualityRepository;

    @Autowired
    private ZakatPaymentRepository zakatPaymentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        zakatPaymentRepository.deleteAll();
        zakatQualityRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_createsZakatPayment_andCalculatesBeratBerasFromQuality() throws Exception {
        ZakatQuality quality = zakatQualityRepository.save(ZakatQuality.builder()
                .name("Beras Premium")
                .zakatType(ZisType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal("2.5"))
                .build());

        Map<String, Object> body = Map.of(
                "jumlahJiwa", 3,
                "alamat", "Jl. Mawar No. 1",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "zakatQualityId", quality.getId().toString(),
                "muzakkiNames", List.of("Ahmad", "Budi", "Siti")
        );

        String responseJson = mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.jumlahJiwa").value(3))
                .andExpect(jsonPath("$.alamat").value("Jl. Mawar No. 1"))
                .andExpect(jsonPath("$.jumlahUang").value(nullValue()))
                .andExpect(jsonPath("$.beratBerasKg").value(7.5))
                .andExpect(jsonPath("$.zakatQuality.id").value(quality.getId().toString()))
                .andExpect(jsonPath("$.zakatQuality.name").value("Beras Premium"))
                .andExpect(jsonPath("$.zakatQuality.beratPerJiwaKg").value(2.5))
                .andExpect(jsonPath("$.muzakkiNames.length()").value(3))
                .andExpect(jsonPath("$.muzakkiNames[0]").value("Ahmad"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());
        ZakatPayment saved = zakatPaymentRepository.findById(paymentId).orElseThrow();
        assertThat(saved.getJumlahJiwa()).isEqualTo(3);
        assertThat(saved.getBeratBerasKg()).isEqualByComparingTo("7.5");
        assertThat(saved.getZakatQuality()).isNotNull();
        assertThat(saved.getZakatQuality().getId()).isEqualTo(quality.getId());

        Long muzakkiCount = entityManager.createQuery("select count(m) from MuzakkiPerson m", Long.class)
                .getSingleResult();
        assertThat(muzakkiCount).isEqualTo(3L);

        Long muzakkiCountByPayment = entityManager.createQuery(
                        "select count(m) from MuzakkiPerson m where m.payment.id = :paymentId",
                        Long.class
                )
                .setParameter("paymentId", paymentId)
                .getSingleResult();
        assertThat(muzakkiCountByPayment).isEqualTo(3L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_returns400_whenNoQualityAndNoBeratBerasAndNoJumlahUang() throws Exception {
        Map<String, Object> body = Map.of(
                "jumlahJiwa", 1,
                "alamat", "Jl. Melati No. 2",
                "zakatType", "ZAKAT_FITRAH_UANG",
                "muzakkiNames", List.of("Ahmad")
        );

        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());

        assertThat(zakatPaymentRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_returns404_whenQualityIdNotFound() throws Exception {
        Map<String, Object> body = Map.of(
                "jumlahJiwa", 2,
                "alamat", "Jl. Kenanga No. 3",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "zakatQualityId", UUID.randomUUID().toString(),
                "muzakkiNames", List.of("Ahmad", "Budi")
        );

        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isNotFound());

        assertThat(zakatPaymentRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_filtersByKeyword_matchesAlamatOrNama() throws Exception {
        ZakatQuality quality = zakatQualityRepository.save(ZakatQuality.builder()
                .name("Standar 2.5 Kg")
                .zakatType(ZisType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal("2.5"))
                .build());

        Map<String, Object> body1 = Map.of(
                "jumlahJiwa", 1,
                "alamat", "Jl. Mawar No. 1",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "zakatQualityId", quality.getId().toString(),
                "muzakkiNames", List.of("Ahmad")
        );
        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body1)))
                .andExpect(status().isCreated());

        Map<String, Object> body2 = Map.of(
                "jumlahJiwa", 1,
                "alamat", "Jl. Melati No. 2",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "zakatQualityId", quality.getId().toString(),
                "muzakkiNames", List.of("Budi")
        );
        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body2)))
                .andExpect(status().isCreated());

        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/zakat-payments")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString())
                        .queryParam("q", "ahmad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].alamat").value("Jl. Mawar No. 1"));

        mockMvc.perform(get("/api/zakat-payments")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString())
                        .queryParam("q", "melati"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].alamat").value("Jl. Melati No. 2"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cancel_marksCanceled_andExcludedFromRekap() throws Exception {
        ZakatQuality quality = zakatQualityRepository.save(ZakatQuality.builder()
                .name("Standar 2.5 Kg")
                .zakatType(ZisType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal("2.5"))
                .build());

        Map<String, Object> body = Map.of(
                "jumlahJiwa", 2,
                "alamat", "Jl. Mawar No. 1",
                "zakatType", "ZAKAT_FITRAH_BERAS",
                "zakatQualityId", quality.getId().toString(),
                "muzakkiNames", List.of("Ahmad", "Budi")
        );

        String createdJson = mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        mockMvc.perform(post("/api/zakat-payments/{id}/cancel", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Salah input"))))
                .andExpect(status().isNoContent());

        assertThat(zakatPaymentRepository.findById(paymentId)).get().extracting("canceled").isEqualTo(true);

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/reports/rekap-zis")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zakatFitrahBerasKg").value(0))
                .andExpect(jsonPath("$.totalMuzakkiFitrahJiwa").value(0));
    }
}
