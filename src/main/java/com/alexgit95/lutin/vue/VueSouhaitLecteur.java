package com.alexgit95.lutin.vue;

import java.math.BigDecimal;

/** Article tel que le voit un lecteur ; {@code prisPar} est nul si l'article est libre. */
public record VueSouhaitLecteur(Long id, String nom, String initiale, String url, String hote, BigDecimal prix,
                                String prisPar) {
}
