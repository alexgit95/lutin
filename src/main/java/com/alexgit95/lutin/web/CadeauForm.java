package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.CadeauCommande;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class CadeauForm {

    private String nom;
    private String url;
    private BigDecimal prix;
    private Long personneId;
    private boolean masque;
    private boolean pourTiers;
    private Set<Long> tagIds = new HashSet<>();

    public static CadeauForm depuis(CadeauCommande c) {
        CadeauForm form = new CadeauForm();
        form.nom = c.nom();
        form.url = c.url();
        form.prix = c.prix();
        form.personneId = c.personneId();
        form.masque = c.masque();
        form.pourTiers = c.pourTiers();
        form.tagIds = new HashSet<>(c.tagIds());
        return form;
    }

    public CadeauCommande versCommande() {
        return new CadeauCommande(nom, url, prix, personneId, masque, pourTiers,
                tagIds == null ? Set.of() : tagIds);
    }
}
