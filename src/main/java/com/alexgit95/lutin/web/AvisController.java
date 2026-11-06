package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.AvisService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class AvisController {

    private final AvisService avis;

    public AvisController(AvisService avis) {
        this.avis = avis;
    }

    /** {@code retour} est un identifiant numérique : aucune redirection vers une adresse libre. */
    @PostMapping("/avis/{id}/fermer")
    public String fermer(@PathVariable Long id, @RequestParam(required = false) Long retour, Principal principal) {
        avis.fermer(principal.getName(), id);
        return retour == null ? "redirect:/wishlists" : "redirect:/wishlists/" + retour;
    }
}
