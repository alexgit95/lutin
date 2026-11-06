package com.alexgit95.lutin.vue;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Tag;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Projection d'un cadeau pour l'affichage ; un cadeau masqué vu par un lecteur n'expose aucune donnée, nom compris. */
public record VueCadeau(Long id, String nom, String url, BigDecimal prix, Statut statut, boolean masque,
                        boolean pourTiers, Long personneId, String personne, List<String> tags) {

    public static VueCadeau depuis(Cadeau c, boolean lecteur) {
        if (lecteur && c.isMasque()) {
            return new VueCadeau(null, null, null, null, null, true, false, null, null, List.of());
        }
        List<String> tags = c.getTags().stream().map(Tag::getNom).sorted(Comparator.naturalOrder()).toList();
        return new VueCadeau(c.getId(), c.getNom(), c.getUrl(), c.getPrix(), c.getStatut(), c.isMasque(),
                c.isPourTiers(), c.getPersonne().getId(), c.getPersonne().getNom(), tags);
    }
}
