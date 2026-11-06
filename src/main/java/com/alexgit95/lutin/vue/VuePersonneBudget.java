package com.alexgit95.lutin.vue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Personne d'une liste avec son budget, ce qui est dépensé (Commandé ou plus) et ce qui reste à payer. */
public record VuePersonneBudget(Long id, String nom, BigDecimal budget, BigDecimal depense,
                                BigDecimal resteAPayer) {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public BigDecimal prevu() {
        return depense.add(resteAPayer);
    }

    /** Sans budget, la barre n'aurait pas de sens : seul le texte est affiché. */
    public boolean sansBarre() {
        return budget.signum() == 0;
    }

    public BigDecimal depassement() {
        return prevu().subtract(budget).max(BigDecimal.ZERO);
    }

    public boolean depasse() {
        return depassement().signum() > 0;
    }

    // La barre grandit avec le prévu quand il dépasse le budget : un repère marque alors le budget.
    private BigDecimal echelle() {
        return prevu().max(budget);
    }

    public BigDecimal pourcentDepense() {
        return pourcent(depense);
    }

    /** Borné pour que les deux segments ne dépassent jamais 100 % malgré les arrondis. */
    public BigDecimal pourcentReste() {
        return pourcent(resteAPayer).min(CENT.subtract(pourcentDepense()));
    }

    public BigDecimal pourcentBudget() {
        return pourcent(budget);
    }

    private BigDecimal pourcent(BigDecimal part) {
        BigDecimal echelle = echelle();
        return echelle.signum() == 0 ? BigDecimal.ZERO
                : part.multiply(CENT).divide(echelle, 1, RoundingMode.HALF_UP);
    }
}
