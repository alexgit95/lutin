package com.alexgit95.lutin.transfert;

import java.util.List;
import java.util.stream.Collectors;

/** Une table exportée : sa lecture JPQL paginée et ses colonnes ; {@code avecId} vaut faux pour la table de jointure. */
public record TableTransfert(String nom, String from, String ordre, boolean avecId, List<Colonne> colonnes) {

    public String requete() {
        return "select " + colonnes.stream().map(Colonne::chemin).collect(Collectors.joining(", "))
                + " from " + from + " order by " + ordre;
    }

    public String insertion() {
        StringBuilder noms = new StringBuilder();
        StringBuilder parametres = new StringBuilder();
        for (int i = 0; i < colonnes.size(); i++) {
            if (i > 0) {
                noms.append(", ");
                parametres.append(", ");
            }
            noms.append(colonnes.get(i).nom());
            parametres.append(":p").append(i);
        }
        return "insert into " + nom + " (" + noms + ") values (" + parametres + ")";
    }

    public int indexDe(String colonne) {
        for (int i = 0; i < colonnes.size(); i++) {
            if (colonnes.get(i).nom().equals(colonne)) {
                return i;
            }
        }
        return -1;
    }
}
