package com.alexgit95.lutin.security;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurs;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurs.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(login));
        return User.withUsername(utilisateur.getLogin())
                .password(utilisateur.getPasswordHash())
                .roles(utilisateur.isAdmin() ? new String[]{"USER", "ADMIN"} : new String[]{"USER"})
                .build();
    }
}
