package com.alexgit95.lutin.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Authentification issue de Cloudflare Access. Le nom est le pseudo (vide avant sa création) : l'email n'y figure jamais,
 * seule son empreinte est gardée à part.
 */
public class AccesAuthentication extends AbstractAuthenticationToken {

    public static final String ROLE_ONBOARDING = "ROLE_ONBOARDING";

    private final String pseudo;
    private final String emailHmac;

    private AccesAuthentication(String pseudo, String emailHmac, Collection<? extends GrantedAuthority> roles) {
        super(roles);
        this.pseudo = pseudo;
        this.emailHmac = emailHmac;
        setAuthenticated(true);
    }

    public static AccesAuthentication compte(String pseudo, String emailHmac, boolean admin) {
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (admin) {
            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new AccesAuthentication(pseudo, emailHmac, roles);
    }

    public static AccesAuthentication sansCompte(String emailHmac) {
        return new AccesAuthentication("", emailHmac, List.of(new SimpleGrantedAuthority(ROLE_ONBOARDING)));
    }

    public String getEmailHmac() {
        return emailHmac;
    }

    public boolean estSansCompte() {
        return getAuthorities().stream().anyMatch(a -> ROLE_ONBOARDING.equals(a.getAuthority()));
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return pseudo;
    }

    @Override
    public String getName() {
        return pseudo;
    }
}
