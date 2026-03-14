package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.ZakatPayment;
import com.zakat.enums.PaymentMethod;
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
class DashboardControllerIntegrationTest {

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
    void summary_returnsTotals_andBreakdown() throws Exception {
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, JAKARTA);

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
        fitrahUang.setPaymentMethod(PaymentMethod.CASH);
        fitrahUang.setPaymentAt(now);
        zakatPaymentRepository.save(fitrahUang);

        ZakatPayment fitrahBeras = new ZakatPayment();
        fitrahBeras.setJumlahJiwa(60);
        fitrahBeras.setAlamat("B");
        fitrahBeras.setZakatQuality(qualityBeras);
        fitrahBeras.setBeratBerasKg(new BigDecimal("149.5"));
        fitrahBeras.setPaymentMethod(PaymentMethod.TRANSFER);
        fitrahBeras.setPaymentAt(now);
        zakatPaymentRepository.save(fitrahBeras);

        ZakatPayment mal = new ZakatPayment();
        mal.setJumlahJiwa(1);
        mal.setAlamat("C");
        mal.setJumlahUangZakatMal(new BigDecimal("3512500"));
        mal.setPaymentMethod(PaymentMethod.TRANSFER);
        mal.setPaymentAt(now);
        zakatPaymentRepository.save(mal);

        String response = mockMvc.perform(get("/api/dashboard/summary")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("fromDate").asText()).isEqualTo(today.toString());
        assertThat(json.get("toDate").asText()).isEqualTo(today.toString());
        assertThat(json.get("totalTransaksi").asLong()).isEqualTo(3L);
        assertThat(json.get("totalUangMasuk").decimalValue()).isEqualByComparingTo("3692500");
        assertThat(json.get("totalUangCash").decimalValue()).isEqualByComparingTo("180000");
        assertThat(json.get("totalUangTransfer").decimalValue()).isEqualByComparingTo("3512500");
        assertThat(json.get("totalBerasKg").decimalValue()).isEqualByComparingTo("149.5");
        assertThat(json.get("totalJiwaFitrah").asLong()).isEqualTo(64L);
        assertThat(json.get("byType").isArray()).isTrue();
        assertThat(json.get("byType").size()).isGreaterThanOrEqualTo(2);

        assertThat(json.get("receiptInfo").isObject()).isTrue();
        assertThat(json.get("receiptInfo").get("nextReceiptNumber").asText()).startsWith("MA/");

        assertThat(json.get("activeQualities").isArray()).isTrue();
        assertThat(json.get("recentPayments").isArray()).isTrue();
        assertThat(json.get("recentPayments").size()).isEqualTo(3);
    }
}
