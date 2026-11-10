package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AlerteRemboursementRepository;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ListeAchatService {

    public record ListeResume(Long id, String nom, String proprietaire) {
    }

    private final ListeAchatRepository listes;
    private final UtilisateurRepository utilisateurs;
    private final PartageRepository partages;
    private final CadeauRepository cadeaux;
    private final PersonneRepository personnes;
    private final TagRepository tags;
    private final AccesListeService acces;
    private final AlerteRemboursementRepository alertes;

    public ListeAchatService(ListeAchatRepository listes, UtilisateurRepository utilisateurs,
                             PartageRepository partages, CadeauRepository cadeaux,
                             PersonneRepository personnes, TagRepository tags, AccesListeService acces,
                             AlerteRemboursementRepository alertes) {
        this.listes = listes;
        this.utilisateurs = utilisateurs;
        this.partages = partages;
        this.cadeaux = cadeaux;
        this.personnes = personnes;
        this.tags = tags;
        this.acces = acces;
        this.alertes = alertes;
    }

    @Transactional(readOnly = true)
    public List<ListeResume> mesListes(String login) {
        return listes.findByProprietaireLoginOrderByNom(login).stream()
                .map(l -> new ListeResume(l.getId(), l.getNom(), login)).toList();
    }

    @Transactional(readOnly = true)
    public List<ListeResume> listesPartagees(String login) {
        return partages.findByLecteurLoginOrderByListeNom(login).stream()
                .map(p -> new ListeResume(p.getListe().getId(), p.getListe().getNom(),
                        p.getListe().getProprietaire().getLogin())).toList();
    }

    @Transactional(readOnly = true)
    public String nom(Long listeId, String login) {
        return acces.proprietaire(listeId, login).getNom();
    }

    public ListeAchat creer(String login, String nom) {
        Utilisateur proprietaire = utilisateurs.findByLogin(login)
                .orElseThrow(() -> new ElementIntrouvableException("Utilisateur inconnu"));
        return listes.save(new ListeAchat(nomValide(nom), proprietaire));
    }

    public void renommer(Long listeId, String login, String nom) {
        acces.proprietaire(listeId, login).setNom(nomValide(nom));
    }

    public void supprimer(Long listeId, String login) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        cadeaux.deleteByListeId(listeId);
        alertes.supprimerDeLaListe(listeId);
        partages.deleteByListeId(listeId);
        tags.deleteByListeId(listeId);
        personnes.deleteByListeId(listeId);
        listes.delete(liste);
    }

    private String nomValide(String nom) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty() || propre.length() > 100) {
            throw new RequeteInvalideException("Le nom de la liste est obligatoire (100 caractères maximum)");
        }
        return propre;
    }
}
