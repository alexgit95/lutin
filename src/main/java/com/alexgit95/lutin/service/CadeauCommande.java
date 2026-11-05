package com.alexgit95.lutin.service;

import java.math.BigDecimal;
import java.util.Set;

public record CadeauCommande(String nom, String url, BigDecimal prix, Long personneId,
                             boolean masque, boolean pourTiers, Set<Long> tagIds) {
}
