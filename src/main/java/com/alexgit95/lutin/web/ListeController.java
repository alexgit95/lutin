package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.service.DashboardService;
import com.alexgit95.lutin.service.ListeAchatService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.vue.VueListe;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ListeController {

    private final ListeAchatService listes;
    private final DashboardService dashboard;

    public ListeController(ListeAchatService listes, DashboardService dashboard) {
        this.listes = listes;
        this.dashboard = dashboard;
    }

    @PostMapping("/listes")
    public String creer(@RequestParam String nom, Principal principal, RedirectAttributes flash) {
        try {
            ListeAchat liste = listes.creer(principal.getName(), nom);
            return "redirect:/listes/" + liste.getId();
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/listes";
        }
    }

    @GetMapping("/listes/{id}")
    public String afficher(@PathVariable Long id, @RequestParam(required = false) Long tag,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "false") boolean aRembourser,
                           Principal principal, Model model) {
        VueListe vue = dashboard.construire(id, principal.getName(), tag, page, aRembourser);
        model.addAttribute("vue", vue);
        model.addAttribute("lot", new DashboardService.LotCadeaux(vue.id(), vue.tagActif(), vue.lecteur(),
            vue.cadeaux(), vue.groupes(), vue.page(), vue.page() + 1 < vue.totalPages(), vue.filtreARembourser()));
        model.addAttribute("statuts", Statut.values());
        return "liste";
    }

    @GetMapping("/listes/{id}/cadeaux")
    public String chargerCadeaux(@PathVariable Long id, @RequestParam(required = false) Long tag,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "false") boolean aRembourser,
                                 Principal principal, Model model) {
        model.addAttribute("lot", dashboard.chargerLot(id, principal.getName(), tag, page, aRembourser));
        model.addAttribute("statuts", Statut.values());
        return "liste-fragments :: cadeauxLot";
    }

    @GetMapping("/listes/{id}/reglages")
    public String reglages(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("listeId", id);
        model.addAttribute("nomListe", listes.nom(id, principal.getName()));
        return "reglages";
    }

    @PostMapping("/listes/{id}/renommer")
    public String renommer(@PathVariable Long id, @RequestParam String nom, Principal principal,
                           RedirectAttributes flash) {
        try {
            listes.renommer(id, principal.getName(), nom);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/listes/" + id + "/reglages";
    }

    @PostMapping("/listes/{id}/supprimer")
    public String supprimer(@PathVariable Long id, Principal principal) {
        listes.supprimer(id, principal.getName());
        return "redirect:/listes";
    }
}
