package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.service.AlerteRemboursementService;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.DashboardService;
import com.alexgit95.lutin.service.RemboursementService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.vue.VueListe;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequestMapping("/listes/{listeId}/cadeaux")
public class CadeauController {

    private final CadeauService cadeaux;
    private final DashboardService dashboard;
    private final RemboursementService remboursements;
    private final AlerteRemboursementService alertes;

    public CadeauController(CadeauService cadeaux, DashboardService dashboard,
                            RemboursementService remboursements, AlerteRemboursementService alertes) {
        this.cadeaux = cadeaux;
        this.dashboard = dashboard;
        this.remboursements = remboursements;
        this.alertes = alertes;
    }

    @GetMapping("/nouveau")
    public String nouveau(@PathVariable Long listeId, @RequestParam(required = false) Long personneId,
                          Principal principal, Model model) {
        CadeauForm form = new CadeauForm();
        form.setPersonneId(personneId);
        return formulaire(model, listeId, null, form, principal);
    }

    @PostMapping
    public String creer(@PathVariable Long listeId, @ModelAttribute("form") CadeauForm form,
                        BindingResult liaison, Principal principal, Model model) {
        if (liaison.hasErrors()) {
            model.addAttribute("erreur", "Prix invalide");
            return formulaire(model, listeId, null, form, principal);
        }
        try {
            cadeaux.creer(listeId, principal.getName(), cadeaux.completerNom(form.versCommande()));
        } catch (RequeteInvalideException e) {
            model.addAttribute("erreur", e.getMessage());
            return formulaire(model, listeId, null, form, principal);
        }
        return "redirect:/listes/" + listeId;
    }

    @GetMapping("/{cadeauId}/modifier")
    public String modifierFormulaire(@PathVariable Long listeId, @PathVariable Long cadeauId,
                                     Principal principal, Model model) {
        CadeauForm form = CadeauForm.depuis(cadeaux.obtenir(listeId, cadeauId, principal.getName()));
        return formulaire(model, listeId, cadeauId, form, principal);
    }

    @PostMapping("/{cadeauId}")
    public String modifier(@PathVariable Long listeId, @PathVariable Long cadeauId,
                           @ModelAttribute("form") CadeauForm form, BindingResult liaison,
                           Principal principal, Model model) {
        if (liaison.hasErrors()) {
            model.addAttribute("erreur", "Prix invalide");
            return formulaire(model, listeId, cadeauId, form, principal);
        }
        try {
            cadeaux.modifier(listeId, cadeauId, principal.getName(), cadeaux.completerNom(form.versCommande()));
        } catch (RequeteInvalideException e) {
            model.addAttribute("erreur", e.getMessage());
            return formulaire(model, listeId, cadeauId, form, principal);
        }
        return "redirect:/listes/" + listeId;
    }

    @PostMapping("/{cadeauId}/supprimer")
    public String supprimer(@PathVariable Long listeId, @PathVariable Long cadeauId, Principal principal) {
        cadeaux.supprimer(listeId, cadeauId, principal.getName());
        return "redirect:/listes/" + listeId;
    }

    /** Changement de statut : fragment HTMX si demandé, sinon retour à la page. */
    @PostMapping("/{cadeauId}/statut")
    public String changerStatut(@PathVariable Long listeId, @PathVariable Long cadeauId,
                                @RequestParam Statut statut, @RequestParam(required = false) Long tag,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestHeader(value = "HX-Request", required = false) String htmx,
                                Principal principal, Model model) {
        cadeaux.changerStatut(listeId, cadeauId, principal.getName(), statut);
        if (htmx == null) {
            return "redirect:/listes/" + listeId;
        }
        VueListe vue = dashboard.construire(listeId, principal.getName(), tag, page);
        model.addAttribute("vue", vue);
        model.addAttribute("lot", new DashboardService.LotCadeaux(vue.id(), vue.tagActif(), vue.lecteur(),
            vue.cadeaux(), vue.groupes(), vue.page(), vue.page() + 1 < vue.totalPages()));
        model.addAttribute("statuts", Statut.values());
        model.addAttribute("cadeauId", cadeauId);
        // Le retour à « À acheter » d'un cadeau remboursé crée une alerte : le badge de l'onglet est rafraîchi.
        model.addAttribute("alertesRemboursement", alertes.compter(listeId, principal.getName()));
        return "liste-fragments :: miseAJourStatut";
    }

    /** Coche ou décoche le remboursement depuis l'onglet : fragment HTMX (ligne et carte) si demandé, sinon retour à l'onglet. */
    @PostMapping("/{cadeauId}/rembourse")
    public String basculerRemboursement(@PathVariable Long listeId, @PathVariable Long cadeauId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestHeader(value = "HX-Request", required = false) String htmx,
                                        Principal principal, Model model) {
        cadeaux.basculerRemboursement(listeId, cadeauId, principal.getName());
        if (htmx == null) {
            return "redirect:/listes/" + listeId + "/remboursements" + (page > 0 ? "?page=" + page : "");
        }
        model.addAttribute("ligne", remboursements.ligne(listeId, cadeauId, principal.getName()));
        model.addAttribute("vue", remboursements.carte(listeId, principal.getName()));
        model.addAttribute("page", page);
        return "remboursement-fragments :: miseAJourRemboursement";
    }

    private String formulaire(Model model, Long listeId, Long cadeauId, CadeauForm form, Principal principal) {
        DashboardService.Options options = dashboard.options(listeId, principal.getName());
        model.addAttribute("prixVerrouille",
                cadeauId != null && cadeaux.estRembourse(listeId, cadeauId, principal.getName()));
        model.addAttribute("form", form);
        model.addAttribute("listeId", listeId);
        model.addAttribute("cadeauId", cadeauId);
        model.addAttribute("personnes", options.personnes());
        model.addAttribute("tags", options.tags());
        return "cadeau-form";
    }
}
