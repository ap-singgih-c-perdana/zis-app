package com.zakat.service.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelZakatPaymentRequest(
        @NotBlank String reason
) {
}

