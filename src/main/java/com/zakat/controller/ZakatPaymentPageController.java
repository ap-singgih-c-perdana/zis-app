package com.zakat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ZakatPaymentPageController {

    @GetMapping("/zakat-payments/new")
    public String newPayment() {
        return "zakat-payment-add";
    }

    @GetMapping("/zakat-payments/list")
    public String listPayments() {
        return "zakat-payment-list";
    }

    @GetMapping("/zakat-payments/{id}/edit")
    public String editPayment(@PathVariable String id) {
        return "zakat-payment-edit";
    }
}
