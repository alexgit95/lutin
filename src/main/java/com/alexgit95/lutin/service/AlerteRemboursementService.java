package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.AlerteRemboursement;
import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.repository.AlerteRemboursementRepository;
import com.alexgit95.lutin.vue.VueListe.VueAlerte;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/** Alertes « virement en trop » : réservées au propriétaire, elles restent jusqu'à leur fermeture. */
@Service
@Transactional
public class AlerteRemboursementService {

    public static final int TAILLE = 20;

    public record Alertes(List<VueAlerte> recentes, long total) {
    }

    private final AlerteRemboursementRepository alertes;
    private final AccesListeService acces;
    private final Clock horloge;

    public AlerteRemboursementService(AlerteRemboursementRepository alertes, AccesListeService acces, Clock horloge) {
        this.alertes = alertes;
        this.acces = acces;
        this.horloge = horloge;
    }

    /** À appeler quand un cadeau remboursé cesse de l'être : le virement effectué devient inutile. */
    public void signaler(ListeAchat liste, String cadeauNom, BigDecimal montant) {
        AlerteRemboursement alerte = new AlerteRemboursement();
        alerte.setListe(liste);
        alerte.setCadeauNom(cadeauNom);
        alerte.setMontant(montant);
        alerte.setCreeLe(horloge.instant());
        alertes.save(alerte);
    }

    @Transactional(readOnly = true)
    public Alertes lister(Long listeId, String login) {
        acces.proprietaire(listeId, login);
        List<VueAlerte> recentes = alertes
                .findByListeIdOrderByCreeLeDescIdDesc(listeId, PageRequest.of(0, TAILLE)).stream()
                .map(a -> new VueAlerte(a.getId(), a.getCadeauNom(), a.getMontant())).toList();
        return new Alertes(recentes, alertes.countByListeId(listeId));
    }

    /** Fermeture définitive ; une alerte absente ou d'une autre liste est ignorée. */
    public void fermer(Long listeId, Long alerteId, String login) {
        acces.proprietaire(listeId, login);
        alertes.findByIdAndListeId(alerteId, listeId).ifPresent(alertes::delete);
    }
}
