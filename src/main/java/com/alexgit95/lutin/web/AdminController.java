package com.alexgit95.lutin.web;

import com.alexgit95.lutin.config.LutinProperties;
import com.alexgit95.lutin.service.CleApiService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final LutinProperties properties;
    private final CleApiService clesApi;
    private final boolean modeCloudflare;

    public AdminController(LutinProperties properties, CleApiService clesApi,
                           @Value("${lutin.acces.mode:local}") String mode) {
        this.properties = properties;
        this.clesApi = clesApi;
        this.modeCloudflare = "cloudflare".equals(mode);
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        return page(model);
    }

    /** La clé en clair n'est rendue que dans cette réponse : ni redirection, ni session, ni cache. */
    @PostMapping("/admin/cles-api")
    public String genererCle(@RequestParam(defaultValue = "") String nom, Model model,
                             RedirectAttributes flash, HttpServletResponse reponse) {
        try {
            model.addAttribute("cleGeneree", clesApi.generer(nom));
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/admin";
        }
        reponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return page(model);
    }

    @PostMapping("/admin/cles-api/{id}/revoquer")
    public String revoquerCle(@PathVariable Long id, RedirectAttributes flash) {
        clesApi.revoquer(id);
        flash.addFlashAttribute("info", "Clé révoquée : elle ne permet plus d'appeler la sauvegarde");
        return "redirect:/admin";
    }

    private String page(Model model) {
        model.addAttribute("modeCloudflare", modeCloudflare);
        // Pas de clé d'invitation en mode cloudflare : l'accès est géré dans Cloudflare Access.
        model.addAttribute("invitationKey", modeCloudflare ? null : properties.invitationKey());
        model.addAttribute("clesApi", clesApi.lister());
        return "admin";
    }
}
