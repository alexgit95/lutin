package com.alexgit95.lutin.service;

public class InscriptionException extends RuntimeException {

    public enum Motif { CLE_INVALIDE, LOGIN_UTILISE }

    private final Motif motif;

    public InscriptionException(Motif motif) {
        super(motif.name());
        this.motif = motif;
    }

    public Motif getMotif() {
        return motif;
    }
}
