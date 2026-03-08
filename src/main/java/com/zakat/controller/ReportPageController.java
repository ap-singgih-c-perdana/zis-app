package com.zakat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportPageController {

    @GetMapping("/reports/rekap")
    public String rekap() {
        return "report-rekap";
    }

    @GetMapping("/reports/muzakki-detail")
    public String muzakkiDetail() {
        return "report-muzakki-detail";
    }
}

