package com.alexgit95.lutin.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutAdvice {

    @ModelAttribute("utilisateurCourant")
    public String utilisateurCourant(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    @ModelAttribute("estAdmin")
    public boolean estAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
