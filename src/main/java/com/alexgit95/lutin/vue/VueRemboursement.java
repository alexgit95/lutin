package com.alexgit95.lutin.vue;

import com.alexgit95.lutin.domain.Statut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Onglet Remboursement d'une liste : réservé au propriétaire, jamais construit pour un lecteur. */
public record VueRemboursement(Long id, String nom, BigDecimal aRembourser, BigDecimal rembourse, LocalDate aujourdhui,
                               List<VueAlerte> alertes, long totalAlertes,
                               List<Ligne> cadeaux, int page, int totalPages) {

    public record VueAlerte(Long id, String cadeauNom, BigDecimal montant) {
    }

    /** Un cadeau remboursable ; {@code rembourseLe} est au format jj/MM, nul tant qu'il n'est pas remboursé. */
    public record Ligne(Long id, String nom, String personne, BigDecimal prix, Statut statut, String rembourseLe) {
    }
}
