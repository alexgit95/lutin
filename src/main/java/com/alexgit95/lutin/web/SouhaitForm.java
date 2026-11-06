package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.SouhaitCommande;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SouhaitForm {

    private String nom;
    private String url;
    private BigDecimal prix;

    public static SouhaitForm depuis(SouhaitCommande c) {
        SouhaitForm form = new SouhaitForm();
        form.nom = c.nom();
        form.url = c.url();
        form.prix = c.prix();
        return form;
    }

    public SouhaitCommande versCommande() {
        return new SouhaitCommande(nom, url, prix);
    }
}
