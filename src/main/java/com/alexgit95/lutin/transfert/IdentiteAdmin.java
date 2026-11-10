package com.alexgit95.lutin.transfert;

/**
 * Identité de l'administrateur qui lance un import : en mode {@code cloudflare} l'empreinte de son email, en mode
 * {@code local} son login ; le fichier doit contenir son compte avec le rôle administrateur.
 */
public record IdentiteAdmin(String login, String emailHmac) {
}
