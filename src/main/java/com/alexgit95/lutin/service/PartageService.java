package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Partage;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PartageService {

    public record VuePartage(Long id, String lecteur) {
    }

    private final PartageRepository partages;
    private final UtilisateurRepository utilisateurs;
    private final AccesListeService acces;

    public PartageService(PartageRepository partages, UtilisateurRepository utilisateurs, AccesListeService acces) {
        this.partages = partages;
        this.utilisateurs = utilisateurs;
        this.acces = acces;
    }

    @Transactional(readOnly = true)
    public List<VuePartage> lister(Long listeId, String login) {
        acces.proprietaire(listeId, login);
        return partages.findByListeIdOrderByLecteurLogin(listeId).stream()
                .map(p -> new VuePartage(p.getId(), p.getLecteur().getLogin())).toList();
    }

    /** Logins proposables : tous les comptes sauf le propriétaire et les lecteurs déjà ajoutés. */
    @Transactional(readOnly = true)
    public List<String> candidats(Long listeId, String login) {
        acces.proprietaire(listeId, login);
        Set<String> exclus = new HashSet<>();
        exclus.add(login);
        partages.findByListeIdOrderByLecteurLogin(listeId).forEach(p -> exclus.add(p.getLecteur().getLogin()));
        return utilisateurs.findAllByOrderByLoginAsc().stream()
                .map(Utilisateur::getLogin).filter(l -> !exclus.contains(l)).toList();
    }

    public void partager(Long listeId, String login, String lecteurLogin) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        Utilisateur lecteur = utilisateurs.findByLogin(lecteurLogin == null ? "" : lecteurLogin.trim())
                .orElseThrow(() -> new RequeteInvalideException("Utilisateur inconnu"));
        if (lecteur.getLogin().equals(login)) {
            throw new RequeteInvalideException("Vous ne pouvez pas partager une liste avec vous-même");
        }
        // Le dialecte SQLite n'applique pas la contrainte composite : unicité vérifiée ici.
        if (partages.existsByListeIdAndLecteurId(listeId, lecteur.getId())) {
            throw new RequeteInvalideException("Cette liste est déjà partagée avec cet utilisateur");
        }
        partages.save(new Partage(liste, lecteur));
    }

    public void retirer(Long listeId, Long partageId, String login) {
        acces.proprietaire(listeId, login);
        Partage partage = partages.findById(partageId)
                .orElseThrow(() -> new ElementIntrouvableException("Partage introuvable"));
        if (!partage.getListe().getId().equals(listeId)) {
            throw new ElementIntrouvableException("Partage introuvable");
        }
        partages.delete(partage);
    }
}
