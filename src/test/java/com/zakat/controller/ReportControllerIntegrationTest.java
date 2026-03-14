package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.ZakatPayment;
import com.zakat.enums.ZisType;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.repository.ZakatQualityRepository;
import com.zakat.entity.ZakatQuality;
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

    @Autowired
    private ZakatQualityRepository zakatQualityRepository;

    @BeforeEach
    void setUp() {
        zakatPaymentRepository.deleteAll();
        zakatQualityRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rekapZis_aggregatesByType_andReturnsTotals() throws Exception {
        Instant now = Instant.now();

        ZakatQuality qualityUang = zakatQualityRepository.save(ZakatQuality.builder()
                .name("Fitrah Uang")
                .zakatType(ZisType.ZAKAT_FITRAH_UANG)
                .nominalPerJiwa(45000L)
                .active(true)
                .build());

        ZakatQuality qualityBeras = zakatQualityRepository.save(ZakatQuality.builder()
                .name("Fitrah Beras")
                .zakatType(ZisType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal("2.4916666666667"))
                .active(true)
                .build());

        ZakatPayment fitrahUang = new ZakatPayment();
        fitrahUang.setJumlahJiwa(4);
        fitrahUang.setAlamat("A");
        fitrahUang.setZakatQuality(qualityUang);
        fitrahUang.setJumlahUang(new BigDecimal("180000"));
        fitrahUang.setPaymentAt(now);
        zakatPaymentRepository.save(fitrahUang);

        ZakatPayment fitrahBeras = new ZakatPayment();
        fitrahBeras.setJumlahJiwa(60);
        fitrahBeras.setAlamat("B");
        fitrahBeras.setZakatQuality(qualityBeras);
        fitrahBeras.setBeratBerasKg(new BigDecimal("149.5"));
        fitrahBeras.setPaymentAt(now);
        zakatPaymentRepository.save(fitrahBeras);

        ZakatPayment mal = new ZakatPayment();
        mal.setJumlahJiwa(1);
        mal.setAlamat("C");
        mal.setJumlahUangZakatMal(new BigDecimal("3512500"));
        mal.setPaymentAt(now);
        zakatPaymentRepository.save(mal);

        ZakatPayment infaq = new ZakatPayment();
        infaq.setJumlahJiwa(1);
        infaq.setAlamat("D");
        infaq.setJumlahUangInfaqSedekah(new BigDecimal("100000"));
        infaq.setPaymentAt(now);
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
