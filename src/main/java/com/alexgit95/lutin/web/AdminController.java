package com.alexgit95.lutin.web;

import com.alexgit95.lutin.config.LutinProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final LutinProperties properties;

    public AdminController(LutinProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("invitationKey", properties.invitationKey());
        return "admin";
    }
}
