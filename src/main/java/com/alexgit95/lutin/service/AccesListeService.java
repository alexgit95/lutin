package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Point unique des contrôles de droits sur une liste (propriétaire / lecteur). */
@Service
@Transactional(readOnly = true)
public class AccesListeService {

    public record Acces(ListeAchat liste, boolean proprietaire) {
    }

    private final ListeAchatRepository listes;
    private final PartageRepository partages;

    public AccesListeService(ListeAchatRepository listes, PartageRepository partages) {
        this.listes = listes;
        this.partages = partages;
    }

    public ListeAchat proprietaire(Long listeId, String login) {
        ListeAchat liste = charger(listeId);
        if (!liste.getProprietaire().getLogin().equals(login)) {
            throw new AccessDeniedException("Liste réservée à son propriétaire");
        }
        return liste;
    }

    public Acces lecture(Long listeId, String login) {
        ListeAchat liste = charger(listeId);
        if (liste.getProprietaire().getLogin().equals(login)) {
            return new Acces(liste, true);
        }
        if (partages.existsByListeIdAndLecteurLogin(listeId, login)) {
            return new Acces(liste, false);
        }
        throw new AccessDeniedException("Liste non accessible");
    }

    // Une liste inexistante donne le même refus qu'une liste non accessible.
    private ListeAchat charger(Long listeId) {
        return listes.findWithProprietaireById(listeId)
                .orElseThrow(() -> new AccessDeniedException("Liste non accessible"));
    }
}
