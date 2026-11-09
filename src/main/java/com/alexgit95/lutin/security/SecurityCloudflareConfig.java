package com.alexgit95.lutin.security;

import com.alexgit95.lutin.config.CloudflareProperties;
import com.alexgit95.lutin.config.ModeCloudflare;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.Arrays;

/** Chaîne de sécurité du mode cloudflare : l'identité vient de Cloudflare Access, jamais d'un formulaire. */
@Configuration
@ModeCloudflare
public class SecurityCloudflareConfig {

    private static final String[] PUBLICS = {"/error", "/p/**", "/css/**", "/js/**", "/icons/**",
            "/manifest.webmanifest", "/sw.js"};

    @Bean
    SecurityFilterChain cloudflareFilterChain(HttpSecurity http, JwtDecoder decodeur, EmailHmacService hmac,
                                              UtilisateurRepository utilisateurs, CloudflareProperties proprietes)
            throws Exception {
        RequestMatcher publics = new OrRequestMatcher(Arrays.stream(PUBLICS)
                .map(p -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(p)).toList());
        CloudflareAccessFilter filtre = new CloudflareAccessFilter(decodeur, hmac, utilisateurs, publics);

        http
                // L'authentification est recalculée à chaque requête : rien n'est enregistré en session.
                .securityContext(contexte -> contexte.securityContextRepository(
                        new RequestAttributeSecurityContextRepository()))
                .addFilterBefore(filtre, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLICS).permitAll()
                        .requestMatchers("/bienvenue").hasRole("ONBOARDING")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().hasRole("USER"))
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint(nonAuthentifie())
                        .accessDeniedHandler(refusAcces()))
                .logout(logout -> logout.logoutSuccessUrl("https://" + proprietes.teamDomain() + "/cdn-cgi/access/logout"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    private static AuthenticationEntryPoint nonAuthentifie() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    /** Un compte sans pseudo est renvoyé vers /bienvenue, un compte existant quitte /bienvenue, le reste est un 403. */
    private static AccessDeniedHandler refusAcces() {
        AccessDeniedHandler parDefaut = new AccessDeniedHandlerImpl();
        return (HttpServletRequest requete, HttpServletResponse reponse,
                org.springframework.security.access.AccessDeniedException erreur) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof AccesAuthentication acces) {
                if (acces.estSansCompte()) {
                    rediriger(requete, reponse, "/bienvenue");
                    return;
                }
                if ((requete.getContextPath() + "/bienvenue").equals(requete.getRequestURI())) {
                    rediriger(requete, reponse, "/");
                    return;
                }
            }
            parDefaut.handle(requete, reponse, erreur);
        };
    }

    private static void rediriger(HttpServletRequest requete, HttpServletResponse reponse, String chemin)
            throws IOException {
        String cible = requete.getContextPath() + chemin;
        if (requete.getHeader("HX-Request") != null) {
            reponse.setHeader("HX-Redirect", cible);
            reponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            reponse.sendRedirect(cible);
        }
    }
}
