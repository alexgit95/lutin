package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.AlerteRemboursementService;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.RemboursementService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.Locale;

/** Remboursement groupé et alertes de virement en trop : propriétaire seulement. */
@Controller
@RequestMapping("/listes/{listeId}")
public class RemboursementController {

    private final CadeauService cadeaux;
    private final AlerteRemboursementService alertes;
    private final RemboursementService remboursements;

    public RemboursementController(CadeauService cadeaux, AlerteRemboursementService alertes,
                                   RemboursementService remboursements) {
        this.cadeaux = cadeaux;
        this.alertes = alertes;
        this.remboursements = remboursements;
    }

    /** Onglet Remboursement : propriétaire seulement (403 sinon). */
    @GetMapping("/remboursements")
    public String onglet(@PathVariable Long listeId, @RequestParam(defaultValue = "0") int page,
                         Principal principal, Model model) {
        model.addAttribute("vue", remboursements.afficher(listeId, principal.getName(), page));
        return "remboursement";
    }

    @PostMapping("/remboursements")
    public String rembourserTout(@PathVariable Long listeId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Principal principal, RedirectAttributes flash) {
        try {
            CadeauService.ResultatRemboursement r = cadeaux.rembourserTout(listeId, principal.getName(), date);
            flash.addFlashAttribute("info", compteRendu(r));
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/listes/" + listeId + "/remboursements";
    }

    @PostMapping("/alertes/{alerteId}/fermer")
    public String fermerAlerte(@PathVariable Long listeId, @PathVariable Long alerteId, Principal principal) {
        alertes.fermer(listeId, alerteId, principal.getName());
        return "redirect:/listes/" + listeId + "/remboursements";
    }

    private static String compteRendu(CadeauService.ResultatRemboursement r) {
        if (r.nombre() == 0) {
            return "Aucun cadeau à marquer remboursé";
        }
        return String.format(Locale.FRANCE, "%d cadeau%s marqué%s remboursé%s (%,.2f €)", r.nombre(),
                r.nombre() > 1 ? "x" : "", r.nombre() > 1 ? "s" : "", r.nombre() > 1 ? "s" : "", r.somme());
    }
}
