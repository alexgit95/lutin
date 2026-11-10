package com.alexgit95.lutin.service;

import com.alexgit95.lutin.config.LutinProperties;
import com.alexgit95.lutin.config.ModeLocal;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@ModeLocal
public class RegistrationService {

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder passwordEncoder;
    private final LutinProperties properties;
    private final TransactionTemplate transaction;

    public RegistrationService(UtilisateurRepository utilisateurs, PasswordEncoder passwordEncoder,
                               LutinProperties properties, PlatformTransactionManager transactionManager) {
        this.utilisateurs = utilisateurs;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public Utilisateur register(String login, String password, String invitationKey) {
        if (!cleValide(invitationKey)) {
            throw new InscriptionException(InscriptionException.Motif.CLE_INVALIDE);
        }
        String loginNettoye = login.trim();
        if (utilisateurs.existsByLogin(loginNettoye)) {
            throw new InscriptionException(InscriptionException.Motif.LOGIN_UTILISE);
        }
        String hash = passwordEncoder.encode(password);
        try {
            return creer(loginNettoye, hash, true);
        } catch (DataAccessException e) {
            // SQLite remonte une JpaSystemException plutôt qu'une DataIntegrityViolationException
            if (utilisateurs.existsByLogin(loginNettoye)) {
                throw new InscriptionException(InscriptionException.Motif.LOGIN_UTILISE);
            }
            // Le rôle admin vient d'être attribué à une inscription concurrente.
            return creer(loginNettoye, hash, false);
        }
    }

    private Utilisateur creer(String login, String hash, boolean adminPossible) {
        return transaction.execute(status -> {
            Utilisateur utilisateur = new Utilisateur(login, hash);
            utilisateur.setAdmin(adminPossible && utilisateurs.count() == 0);
            return utilisateurs.saveAndFlush(utilisateur);
        });
    }

    private boolean cleValide(String cle) {
        String attendue = properties.invitationKey();
        if (cle == null || attendue == null || attendue.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(cle.getBytes(StandardCharsets.UTF_8), attendue.getBytes(StandardCharsets.UTF_8));
    }
}
