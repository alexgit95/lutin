package com.alexgit95.lutin.vue;

import com.alexgit95.lutin.domain.Statut;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record VueListe(Long id, String nom, String proprietaire, boolean lecteur,
                       BigDecimal budgetGlobal, BigDecimal debourse, BigDecimal resteAPayer,
                       Map<Statut, Long> repartition, long totalCadeaux,
                       List<VueTag> tags, List<VueCadeau> cadeaux, List<VueGroupe> groupes,
                       Long tagActif, int page, int totalPages) {

    public record VuePersonne(Long id, String nom, BigDecimal budget) {
    }

    public record VueTag(Long id, String nom) {
    }

    public record VueGroupe(Long personneId, String personne, List<VueCadeau> cadeaux) {
    }
}
