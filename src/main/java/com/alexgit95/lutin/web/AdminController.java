package com.alexgit95.lutin.web;

import com.alexgit95.lutin.config.LutinProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final LutinProperties properties;
    private final boolean modeCloudflare;

    public AdminController(LutinProperties properties, @Value("${lutin.acces.mode:local}") String mode) {
        this.properties = properties;
        this.modeCloudflare = "cloudflare".equals(mode);
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("modeCloudflare", modeCloudflare);
        // Pas de clé d'invitation en mode cloudflare : l'accès est géré dans Cloudflare Access.
        model.addAttribute("invitationKey", modeCloudflare ? null : properties.invitationKey());
        return "admin";
    }
}
