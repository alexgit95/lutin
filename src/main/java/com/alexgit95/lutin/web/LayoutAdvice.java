package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.AlerteRemboursementService;
import com.alexgit95.lutin.service.AvisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
public class LayoutAdvice {

    // Le nombre est borné à 18 chiffres pour tenir dans un Long.
    private static final Pattern PAGE_DE_LISTE = Pattern.compile("^/listes/(\\d{1,18})(/.*)?$");

    private final AvisService avis;
    private final AlerteRemboursementService alertes;

    public LayoutAdvice(AvisService avis, AlerteRemboursementService alertes) {
        this.avis = avis;
        this.alertes = alertes;
    }

    @ModelAttribute("utilisateurCourant")
    public String utilisateurCourant(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    @ModelAttribute("estAdmin")
    public boolean estAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static boolean pageAffichee(Authentication authentication, HttpServletRequest requete) {
        boolean connecte = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !authentication.getName().isEmpty();
        return connecte && "GET".equals(requete.getMethod()) && requete.getHeader("HX-Request") == null;
    }

    /** Compté seulement pour les pages affichées (GET) d'un utilisateur connecté, pas pour les actions ni HTMX. */
    @ModelAttribute("nombreAvis")
    public long nombreAvis(Authentication authentication, HttpServletRequest requete) {
        if (!pageAffichee(authentication, requete)) {
            return 0;
        }
        return avis.compter(authentication.getName());
    }

    /** Badge de l'onglet Remboursement : alertes en attente, pour les pages d'une liste dont l'utilisateur est propriétaire. */
    @ModelAttribute("alertesRemboursement")
    public long alertesRemboursement(Authentication authentication, HttpServletRequest requete) {
        if (!pageAffichee(authentication, requete)) {
            return 0;
        }
        Matcher liste = PAGE_DE_LISTE.matcher(requete.getRequestURI().substring(requete.getContextPath().length()));
        if (!liste.matches()) {
            return 0;
        }
        return alertes.compter(Long.valueOf(liste.group(1)), authentication.getName());
    }
}
