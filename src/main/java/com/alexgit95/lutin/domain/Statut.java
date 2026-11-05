package com.alexgit95.lutin.domain;

public enum Statut {
    A_ACHETER("À acheter"),
    COMMANDE("Commandé"),
    LIVRE("Livré"),
    EMBALLE("Emballé"),
    REMIS("Remis");

    private final String libelle;

    Statut(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
