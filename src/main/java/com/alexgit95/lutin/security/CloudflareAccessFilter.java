package com.alexgit95.lutin.security;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Authentifie chaque requête à partir du jeton Cloudflare Access, sans rien garder en session : un accès retiré dans
 * Cloudflare cesse de fonctionner à la requête suivante. Ni le jeton ni l'email ne sont journalisés.
 */
public class CloudflareAccessFilter extends OncePerRequestFilter {

    public static final String EN_TETE = "Cf-Access-Jwt-Assertion";

    private final JwtDecoder decodeur;
    private final EmailHmacService hmac;
    private final UtilisateurRepository utilisateurs;
    private final RequestMatcher cheminsPublics;
    private final SecurityContextHolderStrategy strategie = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository depot = new RequestAttributeSecurityContextRepository();

    public CloudflareAccessFilter(JwtDecoder decodeur, EmailHmacService hmac, UtilisateurRepository utilisateurs,
                                  RequestMatcher cheminsPublics) {
        this.decodeur = decodeur;
        this.hmac = hmac;
        this.utilisateurs = utilisateurs;
        this.cheminsPublics = cheminsPublics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        Optional<String> email = emailDuJeton(requete.getHeader(EN_TETE));
        if (email.isEmpty()) {
            if (cheminsPublics.matches(requete)) {
                chaine.doFilter(requete, reponse);
            } else {
                refuser(reponse);
            }
            return;
        }

        String empreinte = hmac.hacher(email.get());
        Authentication authentification = utilisateurs.findByEmailHmac(empreinte)
                .map(u -> connu(u, empreinte))
                .orElseGet(() -> AccesAuthentication.sansCompte(empreinte));
        SecurityContext contexte = strategie.createEmptyContext();
        contexte.setAuthentication(authentification);
        strategie.setContext(contexte);
        depot.saveContext(contexte, requete, reponse);
        try {
            chaine.doFilter(requete, reponse);
        } finally {
            strategie.clearContext();
        }
    }

    private static Authentication connu(Utilisateur utilisateur, String empreinte) {
        return AccesAuthentication.compte(utilisateur.getLogin(), empreinte, utilisateur.isAdmin());
    }

    private Optional<String> emailDuJeton(String jeton) {
        if (jeton == null || jeton.isBlank()) {
            return Optional.empty();
        }
        try {
            Jwt jwt = decodeur.decode(jeton);
            String email = jwt.getClaimAsString("email");
            return email == null || email.isBlank() ? Optional.empty() : Optional.of(email);
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private static void refuser(HttpServletResponse reponse) throws IOException {
        reponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        reponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write("Accès refusé : session Cloudflare Access absente ou expirée");
    }
}
