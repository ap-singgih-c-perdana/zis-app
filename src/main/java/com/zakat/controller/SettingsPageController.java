package com.zakat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsPageController {

    @GetMapping("/settings/institution-profile")
    public String institutionProfile(Model model) {
        return "institution-profile";
    }

    @GetMapping("/settings/zakat-qualities")
    public String zakatQualities(Model model) {
        return "zakat-quality";
    }
}
