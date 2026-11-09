package com.alexgit95.lutin.transfert;

import java.util.Set;

/**
 * Une colonne exportée. {@code chemin} est l'expression JPQL lue à l'export ; {@code parent} nomme la table dont
 * la colonne référence l'identifiant (nul s'il n'y a pas de référence contrôlée) ; {@code valeurs} limite un texte
 * à une énumération ; {@code longueur} vaut 0 quand elle n'est pas bornée.
 */
public record Colonne(String nom, String chemin, TypeColonne type, boolean obligatoire, int longueur,
                      String parent, Set<String> valeurs) {
}
