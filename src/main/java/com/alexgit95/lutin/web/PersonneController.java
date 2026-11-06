package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.service.DashboardService;
import com.alexgit95.lutin.service.PersonneService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/listes/{listeId}/personnes")
public class PersonneController {

    private final PersonneService personnes;
    private final DashboardService dashboard;

    public PersonneController(PersonneService personnes, DashboardService dashboard) {
        this.personnes = personnes;
        this.dashboard = dashboard;
    }

    @GetMapping
    public String lister(@PathVariable Long listeId, Principal principal, Model model) {
        model.addAttribute("listeId", listeId);
        model.addAttribute("personnes", personnes.lister(listeId, principal.getName()));
        model.addAttribute("sources", personnes.sourcesPossibles(listeId, principal.getName()));
        return "personnes";
    }

    @GetMapping("/import")
    public String apercuImport(@PathVariable Long listeId, @RequestParam Long source, Principal principal,
                               Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("apercu", personnes.apercuImport(listeId, source, principal.getName()));
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/listes/" + listeId + "/personnes";
        }
        model.addAttribute("listeId", listeId);
        return "personnes-import";
    }

    @PostMapping("/import")
    public String importer(@PathVariable Long listeId, @RequestParam Long source,
                           @RequestParam(required = false) List<Long> ids, Principal principal,
                           RedirectAttributes flash) {
        try {
            PersonneService.ResultatImport r = personnes.importer(listeId, source, principal.getName(), ids);
            flash.addFlashAttribute("info", compteRendu(r));
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/listes/" + listeId + "/personnes";
    }

    private static String compteRendu(PersonneService.ResultatImport r) {
        String importees = r.importees() == 0
                ? "Aucune personne importée"
                : String.format(Locale.FRANCE, "%d personne%s importée%s (%,.2f € de budget)", r.importees(),
                        r.importees() > 1 ? "s" : "", r.importees() > 1 ? "s" : "", r.budgetTotal());
        return r.ignorees() == 0 ? importees
                : importees + ", " + r.ignorees() + (r.ignorees() > 1 ? " ignorées" : " ignorée") + " (déjà présente"
                + (r.ignorees() > 1 ? "s" : "") + ")";
    }

    @PostMapping
    public String creer(@PathVariable Long listeId, @RequestParam String nom,
                        @RequestParam(required = false) BigDecimal budget,
                        Principal principal, RedirectAttributes flash) {
        try {
            personnes.creer(listeId, principal.getName(), nom, budget);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/listes/" + listeId + "/personnes";
    }

    @GetMapping("/{personneId}/modifier")
    public String formulaire(@PathVariable Long listeId, @PathVariable Long personneId,
                             Principal principal, Model model) {
        Personne personne = personnes.obtenir(listeId, personneId, principal.getName());
        model.addAttribute("listeId", listeId);
        model.addAttribute("personneId", personneId);
        model.addAttribute("nom", personne.getNom());
        model.addAttribute("budget", personne.getBudget());
        return "personne-form";
    }

    @PostMapping("/{personneId}")
    public String modifier(@PathVariable Long listeId, @PathVariable Long personneId, @RequestParam String nom,
                           @RequestParam(required = false) BigDecimal budget,
                           Principal principal, RedirectAttributes flash) {
        try {
            personnes.modifier(listeId, personneId, principal.getName(), nom, budget);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/listes/" + listeId + "/personnes/" + personneId + "/modifier";
        }
        return "redirect:/listes/" + listeId + "/personnes";
    }

    @PostMapping("/{personneId}/supprimer")
    public String supprimer(@PathVariable Long listeId, @PathVariable Long personneId, Principal principal) {
        personnes.supprimer(listeId, personneId, principal.getName());
        return "redirect:/listes/" + listeId + "/personnes";
    }
}
