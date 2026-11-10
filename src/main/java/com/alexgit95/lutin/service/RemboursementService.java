package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.vue.VueRemboursement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Onglet Remboursement : montants, alertes et cadeaux remboursables d'une liste, pour son propriétaire seulement. */
@Service
@Transactional(readOnly = true)
public class RemboursementService {

    public static final int TAILLE_PAGE = 50;

    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("dd/MM");

    private final AccesListeService acces;
    private final CadeauRepository cadeaux;
    private final AlerteRemboursementService alertes;
    private final DateDuJour dateDuJour;

    public RemboursementService(AccesListeService acces, CadeauRepository cadeaux,
                                AlerteRemboursementService alertes, DateDuJour dateDuJour) {
        this.acces = acces;
        this.cadeaux = cadeaux;
        this.alertes = alertes;
        this.dateDuJour = dateDuJour;
    }

    /** Page de cadeaux remboursables : les non remboursés d'abord, puis par personne et par nom. */
    public VueRemboursement afficher(Long listeId, String login, int page) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        Page<Cadeau> resultat = cadeaux.findRemboursables(listeId, PageRequest.of(Math.max(page, 0), TAILLE_PAGE));
        List<VueRemboursement.Ligne> lignes = resultat.getContent().stream().map(RemboursementService::ligne).toList();
        return construire(liste, login, lignes, resultat.getNumber(), Math.max(resultat.getTotalPages(), 1));
    }

    /** Montants et alertes sans la liste de cadeaux, pour rafraîchir la carte après un cochage. */
    public VueRemboursement carte(Long listeId, String login) {
        return construire(acces.proprietaire(listeId, login), login, List.of(), 0, 1);
    }

    public VueRemboursement.Ligne ligne(Long listeId, Long cadeauId, String login) {
        acces.proprietaire(listeId, login);
        Cadeau cadeau = cadeaux.findById(cadeauId)
                .filter(c -> c.getListe().getId().equals(listeId))
                .orElseThrow(() -> new ElementIntrouvableException("Cadeau introuvable"));
        return ligne(cadeau);
    }

    private VueRemboursement construire(ListeAchat liste, String login, List<VueRemboursement.Ligne> lignes,
                                        int page, int totalPages) {
        AlerteRemboursementService.Alertes liste20 = alertes.lister(liste.getId(), login);
        return new VueRemboursement(liste.getId(), liste.getNom(),
                cadeaux.sommeARembourser(liste.getId()), cadeaux.sommeRembourse(liste.getId()),
                dateDuJour.aujourdhui(), liste20.recentes(), liste20.total(), lignes, page, totalPages);
    }

    private static VueRemboursement.Ligne ligne(Cadeau c) {
        return new VueRemboursement.Ligne(c.getId(), c.getNom(), c.getPersonne().getNom(), c.getPrix(), c.getStatut(),
                c.getRembourseLe() == null ? null : c.getRembourseLe().format(JOUR_MOIS));
    }
}
