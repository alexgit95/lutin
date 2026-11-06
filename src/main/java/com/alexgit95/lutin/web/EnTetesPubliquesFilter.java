package com.alexgit95.lutin.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Le jeton du lien public ne doit fuiter ni par le Referer, ni par un moteur de recherche, ni par un cache. */
@Component
public class EnTetesPubliquesFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requete) {
        String chemin = requete.getRequestURI().substring(requete.getContextPath().length());
        return !chemin.startsWith("/p/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        reponse.setHeader("Referrer-Policy", "no-referrer");
        reponse.setHeader("X-Robots-Tag", "noindex, nofollow");
        reponse.setHeader("Cache-Control", "no-store");
        chaine.doFilter(requete, reponse);
    }
}
