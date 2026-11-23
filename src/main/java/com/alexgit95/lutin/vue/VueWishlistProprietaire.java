package com.alexgit95.lutin.vue;

import java.math.BigDecimal;
import java.util.List;

public record VueWishlistProprietaire(Long id, String nom, List<VueSouhaitProprietaire> souhaits,
                                      int page, int totalPages, BigDecimal totalPrix, long sansPrix) {
}
