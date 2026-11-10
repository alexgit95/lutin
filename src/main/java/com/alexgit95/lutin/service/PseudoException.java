package com.alexgit95.lutin.service;

public class PseudoException extends RuntimeException {

    public enum Motif { FORME, RESERVE, PRIS }

    private final Motif motif;

    public PseudoException(Motif motif) {
        super(motif.name());
        this.motif = motif;
    }

    public Motif getMotif() {
        return motif;
    }
}
