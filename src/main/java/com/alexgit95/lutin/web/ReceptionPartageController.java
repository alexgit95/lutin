package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.PartageLien;
import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.service.WishlistService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;

@Controller
@RequestMapping("/partage")
public class ReceptionPartageController {

    private final WishlistService wishlists;

    public ReceptionPartageController(WishlistService wishlists) {
        this.wishlists = wishlists;
    }

    @GetMapping
    public String recevoir(@RequestParam(required = false) String url,
                           @RequestParam(required = false) String title,
                           @RequestParam(required = false) String text,
                           Principal principal, Model model, HttpServletResponse reponse) {
        confidentiel(reponse);
        try {
            return choix(PartageLien.depuis(url, title, text), principal, model);
        } catch (RequeteInvalideException erreur) {
            model.addAttribute("erreur", erreur.getMessage());
            return "partage";
        }
    }

    @PostMapping("/wishlists")
    public String creerWishlist(@RequestParam(required = false) String url,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String nom,
                                Principal principal, Model model, HttpServletResponse reponse) {
        confidentiel(reponse);
        PartageLien partage;
        try {
            partage = PartageLien.depuis(url, title, null);
        } catch (RequeteInvalideException erreur) {
            model.addAttribute("erreur", erreur.getMessage());
            return "partage";
        }
        try {
            wishlists.creer(principal.getName(), nom);
        } catch (RequeteInvalideException erreur) {
            model.addAttribute("erreur", erreur.getMessage());
            model.addAttribute("nomWishlist", nom);
            return choix(partage, principal, model);
        }
        return "redirect:" + retour(partage);
    }

    private String choix(PartageLien partage, Principal principal, Model model) {
        model.addAttribute("partage", partage);
        model.addAttribute("mesWishlists", wishlists.mesWishlists(principal.getName()));
        return "partage";
    }

    static void confidentiel(HttpServletResponse reponse) {
        reponse.setHeader("Cache-Control", "no-store");
        reponse.setHeader("Referrer-Policy", "no-referrer");
    }

    static String retour(PartageLien partage) {
        return UriComponentsBuilder.fromPath("/partage")
                .queryParam("url", "{url}").queryParam("title", "{title}")
                .encode().buildAndExpand(partage.url(), partage.titre()).toUriString();
    }
}