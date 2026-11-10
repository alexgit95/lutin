package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Création du compte d'un utilisateur Cloudflare Access à sa première connexion, sous le pseudo qu'il choisit. */
@Service
public class PseudoService {

    public static final int LONGUEUR_MIN = 4;
    public static final int LONGUEUR_MAX = 50;

    private static final Pattern FORME = Pattern.compile("^[A-Za-z0-9._-]{" + LONGUEUR_MIN + "," + LONGUEUR_MAX + "}$");
    private static final Set<String> RESERVES = Set.of("admin", "administrateur", "root", "lutin", "system", "support");

    private final UtilisateurRepository utilisateurs;
    private final TransactionTemplate transaction;

    public PseudoService(UtilisateurRepository utilisateurs, PlatformTransactionManager transactionManager) {
        this.utilisateurs = utilisateurs;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Idempotent : un compte déjà rattaché à cette empreinte est renvoyé tel quel, sans changer son pseudo. */
    public Utilisateur creer(String emailHmac, String pseudoSaisi) {
        String pseudo = pseudoSaisi == null ? "" : pseudoSaisi.trim();
        if (!FORME.matcher(pseudo).matches()) {
            throw new PseudoException(PseudoException.Motif.FORME);
        }
        String cle = pseudo.toLowerCase(Locale.ROOT);
        if (RESERVES.contains(cle)) {
            throw new PseudoException(PseudoException.Motif.RESERVE);
        }
        var existant = utilisateurs.findByEmailHmac(emailHmac);
        if (existant.isPresent()) {
            return existant.get();
        }
        if (utilisateurs.existsByLoginCle(cle)) {
            throw new PseudoException(PseudoException.Motif.PRIS);
        }
        try {
            return enregistrer(pseudo, emailHmac, true);
        } catch (DataAccessException e) {
            // SQLite remonte une JpaSystemException plutôt qu'une violation d'intégrité typée.
            var concurrent = utilisateurs.findByEmailHmac(emailHmac);
            if (concurrent.isPresent()) {
                return concurrent.get();
            }
            if (utilisateurs.existsByLoginCle(cle)) {
                throw new PseudoException(PseudoException.Motif.PRIS);
            }
            // Le rôle administrateur vient d'être attribué à une création concurrente.
            return enregistrer(pseudo, emailHmac, false);
        }
    }

    private Utilisateur enregistrer(String pseudo, String emailHmac, boolean adminPossible) {
        return transaction.execute(status -> {
            Utilisateur utilisateur = new Utilisateur(pseudo, null);
            utilisateur.setEmailHmac(emailHmac);
            utilisateur.setAdmin(adminPossible && utilisateurs.count() == 0);
            return utilisateurs.saveAndFlush(utilisateur);
        });
    }
}
