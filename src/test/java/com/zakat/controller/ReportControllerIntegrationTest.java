package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.ZakatPayment;
import com.zakat.enums.ZakatType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

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
    void rekapZis_aggregatesByType_andReturnsTotals() throws Exception {
        Instant now = Instant.now();

        ZakatPayment fitrahUang = new ZakatPayment();
        fitrahUang.setJumlahJiwa(4);
        fitrahUang.setAlamat("A");
        fitrahUang.setZakatType(ZakatType.ZAKAT_FITRAH_UANG);
        fitrahUang.setJumlahUang(new BigDecimal("180000"));
        fitrahUang.setCreatedAt(now);
        zakatPaymentRepository.save(fitrahUang);

        ZakatPayment fitrahBeras = new ZakatPayment();
        fitrahBeras.setJumlahJiwa(60);
        fitrahBeras.setAlamat("B");
        fitrahBeras.setZakatType(ZakatType.ZAKAT_FITRAH_BERAS);
        fitrahBeras.setBeratBerasKg(new BigDecimal("149.5"));
        fitrahBeras.setCreatedAt(now);
        zakatPaymentRepository.save(fitrahBeras);

        ZakatPayment mal = new ZakatPayment();
        mal.setJumlahJiwa(1);
        mal.setAlamat("C");
        mal.setZakatType(ZakatType.ZAKAT_MAL);
        mal.setJumlahUang(new BigDecimal("3512500"));
        mal.setCreatedAt(now);
        zakatPaymentRepository.save(mal);

        ZakatPayment infaq = new ZakatPayment();
        infaq.setJumlahJiwa(1);
        infaq.setAlamat("D");
        infaq.setZakatType(ZakatType.INFAQ_SEDEKAH);
        infaq.setJumlahUang(new BigDecimal("100000"));
        infaq.setCreatedAt(now);
        zakatPaymentRepository.save(infaq);

        LocalDate todayJakarta = LocalDate.ofInstant(now, JAKARTA);

        String response = mockMvc.perform(get("/api/reports/rekap-zis")
                        .queryParam("from", todayJakarta.toString())
                        .queryParam("to", todayJakarta.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("fromDate").asText()).isEqualTo(todayJakarta.toString());
        assertThat(json.get("toDate").asText()).isEqualTo(todayJakarta.toString());
        assertThat(json.get("zakatFitrahUang").decimalValue()).isEqualByComparingTo("180000");
        assertThat(json.get("zakatFitrahBerasKg").decimalValue()).isEqualByComparingTo("149.5");
        assertThat(json.get("zakatMal").decimalValue()).isEqualByComparingTo("3512500");
        assertThat(json.get("infaqSedekah").decimalValue()).isEqualByComparingTo("100000");
        assertThat(json.get("totalUangMasuk").decimalValue()).isEqualByComparingTo("3792500");
        assertThat(json.get("totalMuzakkiFitrahJiwa").asLong()).isEqualTo(64L);
    }
}
