package com.alexgit95.lutin.vue;

import java.math.BigDecimal;
import java.util.List;

public record VueWishlistLecteur(Long id, String nom, String proprietaire, List<VueSouhaitLecteur> souhaits,
                                 int page, int totalPages, BigDecimal totalPrix, long sansPrix) {
}
