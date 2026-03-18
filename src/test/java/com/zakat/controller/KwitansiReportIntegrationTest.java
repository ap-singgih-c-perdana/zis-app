package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.MuzakkiPerson;
import com.zakat.entity.ZakatPayment;
import com.zakat.entity.ZakatQuality;
import com.zakat.enums.ZisType;
import com.zakat.repository.ZakatPaymentRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KwitansiReportIntegrationTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    private static final String RECEIPT_NUMBER_FORMAT = "MA/%d/%06d";

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
    void kwitansi_returnsPaymentAndMuzakkiList() throws Exception {
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, JAKARTA);

        ZakatQuality quality = zakatQualityRepository.save(ZakatQuality.builder()
                .name("Fitrah Uang")
                .zakatType(ZisType.ZAKAT_FITRAH_UANG)
                .nominalPerJiwa(45000L)
                .active(true)
                .build());

        ZakatPayment payment = new ZakatPayment();
        payment.setJumlahJiwa(4);
        payment.setAlamat("Jl. Mawar No. 1");
        payment.setReceivedByName("Amil Kwitansi");
        payment.setZakatQuality(quality);
        payment.setJumlahUang(new BigDecimal("180000"));
        payment.setPaymentAt(now);
        payment.setReceiptYear(today.getYear());
        payment.setReceiptSequence(123L);
        payment.setReceiptNumber(String.format(RECEIPT_NUMBER_FORMAT, today.getYear(), 123));
        payment.setMuzakkiList(List.of(
                MuzakkiPerson.builder().nama("Eko Yulianto").payment(payment).build(),
                MuzakkiPerson.builder().nama("Nur Pujianto").payment(payment).build()
        ));
        UUID id = zakatPaymentRepository.save(payment).getId();

        String response = mockMvc.perform(get("/api/reports/kwitansi/{paymentId}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("paymentId").asText()).isEqualTo(id.toString());
        assertThat(json.get("receiptNumber").asText()).startsWith("MA/");
        assertThat(json.get("receiptNumber").asText()).contains("/" + today.getYear() + "/");
        assertThat(json.get("tanggal").asText()).isEqualTo(today.toString());
        assertThat(json.get("zakatType").asText()).isEqualTo("ZAKAT_FITRAH_UANG");
        assertThat(json.get("receivedByName").asText()).isEqualTo("Amil Kwitansi");
        assertThat(json.get("nominalRp").decimalValue()).isEqualByComparingTo("180000");
        assertThat(json.get("muzakkiCount").asInt()).isEqualTo(2);
        assertThat(json.get("muzakkiNames").size()).isEqualTo(2);
    }
}
