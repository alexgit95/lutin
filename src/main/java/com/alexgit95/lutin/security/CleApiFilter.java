package com.alexgit95.lutin.security;

import com.alexgit95.lutin.service.CleApiService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentifie une requête {@code /api/**} par {@code Authorization: Bearer <clé>}, sans session. Une clé absente ou
 * invalide laisse la requête anonyme : le point d'entrée répond alors 401 sans dire pourquoi. La valeur de
 * l'en-tête n'est jamais journalisée.
 */
public class CleApiFilter extends OncePerRequestFilter {

    public static final String ROLE = "ROLE_SAUVEGARDE";
    private static final String SCHEMA = "Bearer ";

    private final CleApiService cles;
    private final SecurityContextHolderStrategy strategie = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository depot = new RequestAttributeSecurityContextRepository();

    public CleApiFilter(CleApiService cles) {
        this.cles = cles;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String entete = requete.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete != null && entete.regionMatches(true, 0, SCHEMA, 0, SCHEMA.length())) {
            cles.authentifier(entete.substring(SCHEMA.length()).trim()).ifPresent(nom -> {
                SecurityContext contexte = strategie.createEmptyContext();
                contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        nom, null, List.of(new SimpleGrantedAuthority(ROLE))));
                strategie.setContext(contexte);
                depot.saveContext(contexte, requete, reponse);
            });
        }
        try {
            chaine.doFilter(requete, reponse);
        } finally {
            strategie.clearContext();
        }
    }
}
