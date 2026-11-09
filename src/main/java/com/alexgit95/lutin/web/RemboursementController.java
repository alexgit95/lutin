package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.AlerteRemboursementService;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
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

    public RemboursementController(CadeauService cadeaux, AlerteRemboursementService alertes) {
        this.cadeaux = cadeaux;
        this.alertes = alertes;
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
        return "redirect:/listes/" + listeId;
    }

    @PostMapping("/alertes/{alerteId}/fermer")
    public String fermerAlerte(@PathVariable Long listeId, @PathVariable Long alerteId, Principal principal) {
        alertes.fermer(listeId, alerteId, principal.getName());
        return "redirect:/listes/" + listeId;
    }

    private static String compteRendu(CadeauService.ResultatRemboursement r) {
        if (r.nombre() == 0) {
            return "Aucun cadeau à marquer remboursé";
        }
        return String.format(Locale.FRANCE, "%d cadeau%s marqué%s remboursé%s (%,.2f €)", r.nombre(),
                r.nombre() > 1 ? "x" : "", r.nombre() > 1 ? "s" : "", r.nombre() > 1 ? "s" : "", r.somme());
    }
}
