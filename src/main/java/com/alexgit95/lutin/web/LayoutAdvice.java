package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.AvisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutAdvice {

    private final AvisService avis;

    public LayoutAdvice(AvisService avis) {
        this.avis = avis;
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

    /** Compté seulement pour les pages affichées (GET) d'un utilisateur connecté, pas pour les actions ni HTMX. */
    @ModelAttribute("nombreAvis")
    public long nombreAvis(Authentication authentication, HttpServletRequest requete) {
        boolean connecte = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (!connecte || !"GET".equals(requete.getMethod()) || requete.getHeader("HX-Request") != null) {
            return 0;
        }
        return avis.compter(authentication.getName());
    }
}
