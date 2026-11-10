package com.alexgit95.lutin.web;

import com.alexgit95.lutin.config.ModeCloudflare;
import com.alexgit95.lutin.security.AccesAuthentication;
import com.alexgit95.lutin.service.PseudoException;
import com.alexgit95.lutin.service.PseudoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Première connexion : choix du pseudo, réservé aux comptes Cloudflare Access qui n'en ont pas encore. */
@Controller
@ModeCloudflare
@RequestMapping("/bienvenue")
public class BienvenueController {

    private final PseudoService pseudos;

    public BienvenueController(PseudoService pseudos) {
        this.pseudos = pseudos;
    }

    @GetMapping
    public String formulaire() {
        return "bienvenue";
    }

    @PostMapping
    public String choisir(@RequestParam(defaultValue = "") String pseudo, Authentication authentication, Model model) {
        AccesAuthentication acces = (AccesAuthentication) authentication;
        try {
            pseudos.creer(acces.getEmailHmac(), pseudo);
        } catch (PseudoException e) {
            model.addAttribute("pseudo", pseudo);
            model.addAttribute("erreur", switch (e.getMotif()) {
                case FORME -> "Entre " + PseudoService.LONGUEUR_MIN + " et " + PseudoService.LONGUEUR_MAX
                        + " caractères : lettres, chiffres, point, tiret et underscore uniquement";
                case RESERVE -> "Ce pseudo est réservé";
                case PRIS -> "Ce pseudo est déjà pris";
            });
            return "bienvenue";
        }
        return "redirect:/";
    }
}
