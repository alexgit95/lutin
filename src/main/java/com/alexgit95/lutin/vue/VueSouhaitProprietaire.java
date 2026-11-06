package com.alexgit95.lutin.vue;

import java.math.BigDecimal;

/** Article tel que le voit le propriétaire : aucun champ lié à l'état « pris ». */
public record VueSouhaitProprietaire(Long id, String nom, String initiale, String url, String hote, BigDecimal prix) {
}
