package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.MuzakkiPerson;
import com.zakat.entity.ZakatPayment;
import com.zakat.enums.ZisType;
import com.zakat.repository.ZakatPaymentRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MuzakkiDetailReportIntegrationTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZakatPaymentRepository zakatPaymentRepository;

    @BeforeEach
    void setUp() {
        zakatPaymentRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void muzakkiDetail_returnsFlattenedRows() throws Exception {
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, JAKARTA);

        ZakatPayment p1 = new ZakatPayment();
        p1.setJumlahJiwa(4);
        p1.setAlamat("A");
        p1.setZakatType(ZisType.ZAKAT_FITRAH_UANG);
        p1.setJumlahUang(new BigDecimal("180000"));
        p1.setCreatedAt(now);
        p1.setMuzakkiList(List.of(
                MuzakkiPerson.builder().nama("Eko Yulianto").payment(p1).build()
        ));
        zakatPaymentRepository.save(p1);

        ZakatPayment p2 = new ZakatPayment();
        p2.setJumlahJiwa(5);
        p2.setAlamat("B");
        p2.setZakatType(ZisType.ZAKAT_FITRAH_BERAS);
        p2.setBeratBerasKg(new BigDecimal("12.50"));
        p2.setCreatedAt(now);
        p2.setMuzakkiList(List.of(
                MuzakkiPerson.builder().nama("Nur Pujianto").payment(p2).build(),
                MuzakkiPerson.builder().nama("Haryoko").payment(p2).build()
        ));
        zakatPaymentRepository.save(p2);

        String response = mockMvc.perform(get("/api/reports/muzakki-detail")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("fromDate").asText()).isEqualTo(today.toString());
        assertThat(json.get("rows").isArray()).isTrue();
        assertThat(json.get("rows").size()).isEqualTo(3);
        assertThat(json.get("totalNominalRp").decimalValue()).isEqualByComparingTo("180000");
        assertThat(json.get("totalBerasKg").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(json.get("totalJiwa").asLong()).isEqualTo(9L);

        JsonNode first = json.get("rows").get(0);
        assertThat(first.get("no").asInt()).isEqualTo(1);
        assertThat(first.get("tanggal").asText()).isEqualTo(today.toString());
        assertThat(first.get("namaMuzakki").asText()).isNotBlank();
        assertThat(first.get("zakatType").asText()).isNotBlank();
    }
}
