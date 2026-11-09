package com.alexgit95.lutin.vue;

import com.alexgit95.lutin.domain.Statut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Les champs de remboursement ({@code aRembourser} à {@code totalAlertes}) sont réservés au propriétaire :
 * ils valent nul, faux ou vide pour un lecteur.
 */
public record VueListe(Long id, String nom, String proprietaire, boolean lecteur,
                       BigDecimal budgetGlobal, BigDecimal debourse, BigDecimal resteAPayer,
                       Map<Statut, Long> repartition, long totalCadeaux,
                       List<VueTag> tags, List<VueCadeau> cadeaux, List<VueGroupe> groupes,
                       Long tagActif, int page, int totalPages,
                       BigDecimal aRembourser, BigDecimal rembourse, LocalDate aujourdhui,
                       boolean filtreARembourser, List<VueAlerte> alertes, long totalAlertes) {

    public record VueAlerte(Long id, String cadeauNom, BigDecimal montant) {
    }

    public record VuePersonne(Long id, String nom, BigDecimal budget) {
    }

    public record VueTag(Long id, String nom) {
    }

    public record VueGroupe(Long personneId, String personne, List<VueCadeau> cadeaux) {
    }
}
