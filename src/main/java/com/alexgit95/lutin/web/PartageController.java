package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.PartageService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class PartageController {

    private final PartageService partages;

    public PartageController(PartageService partages) {
        this.partages = partages;
    }

    @GetMapping("/listes/{listeId}/partages")
    public String afficher(@PathVariable Long listeId, Principal principal, Model model) {
        model.addAttribute("listeId", listeId);
        model.addAttribute("partages", partages.lister(listeId, principal.getName()));
        model.addAttribute("candidats", partages.candidats(listeId, principal.getName()));
        return "partages";
    }

    @PostMapping("/listes/{listeId}/partages")
    public String partager(@PathVariable Long listeId, @RequestParam String lecteur, Principal principal,
                           RedirectAttributes flash) {
        try {
            partages.partager(listeId, principal.getName(), lecteur);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/listes/" + listeId + "/partages";
    }

    @PostMapping("/listes/{listeId}/partages/{partageId}/retirer")
    public String retirer(@PathVariable Long listeId, @PathVariable Long partageId, Principal principal) {
        partages.retirer(listeId, partageId, principal.getName());
        return "redirect:/listes/" + listeId + "/partages";
    }
}
