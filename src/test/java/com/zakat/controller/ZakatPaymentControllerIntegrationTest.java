package com.zakat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakat.entity.ZakatPayment;
import com.zakat.entity.ZakatQuality;
import com.zakat.enums.PaymentMethod;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ZakatPaymentControllerIntegrationTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

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
    void post_createsFitrahBeras_andCalculatesBeratBerasFromQuality() throws Exception {
        ZakatQuality quality = createQualityBeras("Beras Premium", "2.5");
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body = baseCreateBody(today, 3, "Jl. Mawar No. 1", "Doni", PaymentMethod.CASH, List.of("Doni", "Budi", "Siti"));
        body.put("zakatQualityId", quality.getId().toString());

        String responseJson = mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.jumlahJiwa").value(3))
                .andExpect(jsonPath("$.alamat").value("Jl. Mawar No. 1"))
                .andExpect(jsonPath("$.payerName").value("Doni"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.jumlahUang").value(nullValue()))
                .andExpect(jsonPath("$.beratBerasKg").value(7.5))
                .andExpect(jsonPath("$.zakatQuality.id").value(quality.getId().toString()))
                .andExpect(jsonPath("$.muzakkiNames.length()").value(3))
                .andExpect(jsonPath("$.muzakkiNames[0]").value("Doni"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());
        ZakatPayment saved = zakatPaymentRepository.findById(paymentId).orElseThrow();
        assertThat(saved.getJumlahJiwa()).isEqualTo(3);
        assertThat(saved.getBeratBerasKg()).isEqualByComparingTo("7.5");
        assertThat(saved.getZakatQuality()).isNotNull();
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_createsNonFitrah_withMinimalMalOnly() throws Exception {
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body = baseCreateBody(today, 1, "Jl. Melati No. 2", "Rina", PaymentMethod.TRANSFER, List.of());
        body.put("jumlahUangZakatMal", 2500000);

        String responseJson = mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zakatType").value("ZAKAT_MAL"))
                .andExpect(jsonPath("$.zakatQuality").doesNotExist())
                .andExpect(jsonPath("$.jumlahUangZakatMal").value(2500000))
                .andExpect(jsonPath("$.muzakkiNames", hasSize(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());
        ZakatPayment saved = zakatPaymentRepository.findById(paymentId).orElseThrow();
        assertThat(saved.getJumlahUangZakatMal()).isEqualByComparingTo("2500000");
        assertThat(saved.getZakatQuality()).isNull();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_returns400_whenFitrahMuzakkiCountMismatch() throws Exception {
        ZakatQuality quality = createQualityUang("Fitrah Uang", 45000L);
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body = baseCreateBody(today, 3, "Jl. Anggrek", "Andi", PaymentMethod.CASH, List.of("Andi", "Budi"));
        body.put("zakatQualityId", quality.getId().toString());

        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_returns404_whenQualityIdNotFound() throws Exception {
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body = baseCreateBody(today, 2, "Jl. Kenanga No. 3", "Hana", PaymentMethod.TRANSFER, List.of("Hana", "Budi"));
        body.put("zakatQualityId", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_returns400_whenNoFitrahAndAllNominalEmpty() throws Exception {
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body = baseCreateBody(today, 1, "Jl. Dahlia", "Yusuf", PaymentMethod.CASH, List.of());

        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void post_returns400_whenPaymentDateInFuture() throws Exception {
        LocalDate futureDate = LocalDate.now(JAKARTA).plusDays(1);

        Map<String, Object> body = baseCreateBody(futureDate, 1, "Jl. Cempaka", "Ari", PaymentMethod.CASH, List.of());
        body.put("jumlahUangZakatMal", 100000);

        mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void put_updatesToFitrahUang_andReplacesMuzakkiWithoutUniqueConstraintConflict() throws Exception {
        ZakatQuality qualityBeras = createQualityBeras("Beras", "2.5");
        ZakatQuality qualityUang = createQualityUang("Uang", 50000L);
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> createBody = baseCreateBody(today, 3, "Jl. Asoka", "Lina", PaymentMethod.CASH, List.of("Lina", "Budi", "Siti"));
        createBody.put("zakatQualityId", qualityBeras.getId().toString());
        UUID paymentId = createPayment(createBody);

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("paymentDate", today.toString());
        updateBody.put("alamat", "Jl. Asoka Blok B");
        updateBody.put("payerName", "Lina Update");
        updateBody.put("payerPhone", "08123");
        updateBody.put("paymentMethod", "TRANSFER");
        updateBody.put("zakatQualityId", qualityUang.getId().toString());
        updateBody.put("muzakkiNames", List.of("Lina Update", "Bayu", "Rani"));
        updateBody.put("jumlahUangZakatMal", 150000);

        mockMvc.perform(put("/api/zakat-payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jumlahUang").value(150000))
                .andExpect(jsonPath("$.beratBerasKg").value(nullValue()))
                .andExpect(jsonPath("$.paymentMethod").value("TRANSFER"))
                .andExpect(jsonPath("$.muzakkiNames", hasSize(3)))
                .andExpect(jsonPath("$.muzakkiNames[0]").value("Lina Update"));

        Long seqOneCount = entityManager.createQuery(
                        "select count(m) from MuzakkiPerson m where m.payment.id = :paymentId and m.sequenceNo = 1",
                        Long.class
                )
                .setParameter("paymentId", paymentId)
                .getSingleResult();
        assertThat(seqOneCount).isEqualTo(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void put_updatesToNonFitrah_withMinimalInfaqOnly() throws Exception {
        ZakatQuality quality = createQualityUang("Fitrah Uang", 45000L);
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> createBody = baseCreateBody(today, 2, "Jl. Flamboyan", "Rafi", PaymentMethod.CASH, List.of("Rafi", "Nia"));
        createBody.put("zakatQualityId", quality.getId().toString());
        UUID paymentId = createPayment(createBody);

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("paymentDate", today.toString());
        updateBody.put("alamat", "Jl. Flamboyan Timur");
        updateBody.put("payerName", "Rafi");
        updateBody.put("payerPhone", "08999");
        updateBody.put("paymentMethod", "CASH");
        updateBody.put("muzakkiNames", List.of());
        updateBody.put("jumlahUangInfaqSedekah", 75000);

        mockMvc.perform(put("/api/zakat-payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zakatType").value("INFAQ_SEDEKAH"))
                .andExpect(jsonPath("$.jumlahUangInfaqSedekah").value(75000))
                .andExpect(jsonPath("$.zakatQuality").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void put_returns400_whenNonFitrahWithoutAnyNominal() throws Exception {
        ZakatQuality quality = createQualityUang("Fitrah Uang", 45000L);
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> createBody = baseCreateBody(today, 1, "Jl. Randu", "Bima", PaymentMethod.CASH, List.of("Bima"));
        createBody.put("zakatQualityId", quality.getId().toString());
        UUID paymentId = createPayment(createBody);

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("paymentDate", today.toString());
        updateBody.put("alamat", "Jl. Randu Barat");
        updateBody.put("payerName", "Bima");
        updateBody.put("payerPhone", "08111");
        updateBody.put("paymentMethod", "CASH");
        updateBody.put("muzakkiNames", List.of());

        mockMvc.perform(put("/api/zakat-payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void put_returns400_whenPaymentIsCanceled() throws Exception {
        ZakatQuality quality = createQualityBeras("Beras", "2.5");
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> createBody = baseCreateBody(today, 2, "Jl. Cemara", "Rudi", PaymentMethod.CASH, List.of("Rudi", "Bambang"));
        createBody.put("zakatQualityId", quality.getId().toString());
        UUID paymentId = createPayment(createBody);

        mockMvc.perform(post("/api/zakat-payments/{id}/cancel", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Salah input"))))
                .andExpect(status().isNoContent());

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("paymentDate", today.toString());
        updateBody.put("alamat", "Jl. Cemara Utara");
        updateBody.put("payerName", "Rudi");
        updateBody.put("payerPhone", "081234");
        updateBody.put("paymentMethod", "TRANSFER");
        updateBody.put("muzakkiNames", List.of("Rudi", "Bambang"));
        updateBody.put("jumlahUangInfaqSedekah", 50000);

        mockMvc.perform(put("/api/zakat-payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_filtersByKeyword_matchesAlamatOrNama() throws Exception {
        ZakatQuality quality = createQualityBeras("Standar 2.5 Kg", "2.5");
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body1 = baseCreateBody(today, 1, "Jl. Mawar No. 1", "Ahmad", PaymentMethod.CASH, List.of("Ahmad"));
        body1.put("zakatQualityId", quality.getId().toString());
        createPayment(body1);

        Map<String, Object> body2 = baseCreateBody(today, 1, "Jl. Melati No. 2", "Budi", PaymentMethod.TRANSFER, List.of("Budi"));
        body2.put("zakatQualityId", quality.getId().toString());
        createPayment(body2);

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
        ZakatQuality quality = createQualityBeras("Standar 2.5 Kg", "2.5");
        LocalDate today = LocalDate.now(JAKARTA);

        Map<String, Object> body = baseCreateBody(today, 2, "Jl. Mawar No. 1", "Ahmad", PaymentMethod.CASH, List.of("Ahmad", "Budi"));
        body.put("zakatQualityId", quality.getId().toString());
        UUID paymentId = createPayment(body);

        mockMvc.perform(post("/api/zakat-payments/{id}/cancel", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Salah input"))))
                .andExpect(status().isNoContent());

        assertThat(zakatPaymentRepository.findById(paymentId)).get().extracting("canceled").isEqualTo(true);

        mockMvc.perform(get("/api/reports/rekap-zis")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zakatFitrahBerasKg").value(0))
                .andExpect(jsonPath("$.totalMuzakkiFitrahJiwa").value(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_supportsSortingForReceiptCreatedAndNominalColumns() throws Exception {
        LocalDate today = LocalDate.now(JAKARTA);
        LocalDate from = today.minusDays(7);

        Map<String, Object> a = baseCreateBody(today.minusDays(2), 1, "Jl. A", "A", PaymentMethod.CASH, List.of());
        a.put("jumlahUang", 30000);
        a.put("beratBerasKg", 2.0);
        a.put("jumlahUangZakatMal", 100000);
        a.put("jumlahUangInfaqSedekah", 50000);
        a.put("jumlahUangFidiah", 70000);
        UUID idA = createPayment(a);

        Map<String, Object> b = baseCreateBody(today.minusDays(1), 1, "Jl. B", "B", PaymentMethod.CASH, List.of());
        b.put("jumlahUang", 10000);
        b.put("beratBerasKg", 1.0);
        b.put("jumlahUangZakatMal", 300000);
        b.put("jumlahUangInfaqSedekah", 10000);
        b.put("jumlahUangFidiah", 30000);
        UUID idB = createPayment(b);

        Map<String, Object> c = baseCreateBody(today, 1, "Jl. C", "C", PaymentMethod.CASH, List.of());
        c.put("jumlahUang", 20000);
        c.put("beratBerasKg", 3.0);
        c.put("jumlahUangZakatMal", 200000);
        c.put("jumlahUangInfaqSedekah", 20000);
        c.put("jumlahUangFidiah", 10000);
        UUID idC = createPayment(c);

        assertFirstIdBySort(from, today, "receiptNumber", "asc", idA);
        assertFirstIdBySort(from, today, "paymentAt", "asc", idA);
        assertFirstIdBySort(from, today, "jumlahUang", "asc", idB);
        assertFirstIdBySort(from, today, "beratBerasKg", "asc", idB);
        assertFirstIdBySort(from, today, "jumlahUangZakatMal", "asc", idA);
        assertFirstIdBySort(from, today, "jumlahUangInfaqSedekah", "asc", idB);
        assertFirstIdBySort(from, today, "jumlahUangFidiah", "asc", idC);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_sortJumlahUangDesc_placesNullAtBottom() throws Exception {
        LocalDate today = LocalDate.now(JAKARTA);
        LocalDate from = today.minusDays(7);

        Map<String, Object> withValue = baseCreateBody(today.minusDays(1), 1, "Jl. Nilai", "N", PaymentMethod.CASH, List.of());
        withValue.put("jumlahUang", 50000);
        UUID withValueId = createPayment(withValue);

        Map<String, Object> nullValue = baseCreateBody(today, 1, "Jl. Kosong", "K", PaymentMethod.CASH, List.of());
        nullValue.put("jumlahUangZakatMal", 100000);
        UUID nullValueId = createPayment(nullValue);

        mockMvc.perform(get("/api/zakat-payments")
                        .queryParam("from", from.toString())
                        .queryParam("to", today.toString())
                        .queryParam("size", "20")
                        .queryParam("sort", "jumlahUang,desc")
                        .queryParam("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(withValueId.toString()))
                .andExpect(jsonPath("$.content[1].id").value(nullValueId.toString()));
    }

    private ZakatQuality createQualityBeras(String name, String beratPerJiwa) {
        return zakatQualityRepository.save(ZakatQuality.builder()
                .name(name)
                .zakatType(ZisType.ZAKAT_FITRAH_BERAS)
                .beratPerJiwaKg(new BigDecimal(beratPerJiwa))
                .active(true)
                .build());
    }

    private ZakatQuality createQualityUang(String name, long nominalPerJiwa) {
        return zakatQualityRepository.save(ZakatQuality.builder()
                .name(name)
                .zakatType(ZisType.ZAKAT_FITRAH_UANG)
                .nominalPerJiwa(nominalPerJiwa)
                .active(true)
                .build());
    }

    private Map<String, Object> baseCreateBody(
            LocalDate paymentDate,
            int jumlahJiwa,
            String alamat,
            String payerName,
            PaymentMethod paymentMethod,
            List<String> muzakkiNames
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentDate", paymentDate.toString());
        body.put("jumlahJiwa", jumlahJiwa);
        body.put("alamat", alamat);
        body.put("payerName", payerName);
        body.put("payerPhone", "08123456789");
        body.put("paymentMethod", paymentMethod.name());
        body.put("muzakkiNames", new ArrayList<>(muzakkiNames));
        return body;
    }

    private UUID createPayment(Map<String, Object> body) throws Exception {
        String responseJson = mockMvc.perform(post("/api/zakat-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(responseJson);
        return UUID.fromString(json.get("id").asText());
    }

    private void assertFirstIdBySort(LocalDate from, LocalDate to, String sortKey, String direction, UUID expectedId) throws Exception {
        mockMvc.perform(get("/api/zakat-payments")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("size", "20")
                        .queryParam("sort", sortKey + "," + direction)
                        .queryParam("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(expectedId.toString()));
    }
}
