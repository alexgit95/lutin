package com.alexgit95.lutin.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PartageLien(String url, String titre) {

    private static final Pattern LIEN = Pattern.compile("https?://[^\\s\\p{Z}\\p{Cc}<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESPACES = Pattern.compile("[\\s\\p{Z}\\p{Cc}\\p{Cf}]+");

    public static PartageLien depuis(String url, String titre, String texte) {
        verifierTaille(url, 2048);
        verifierTaille(titre, 2048);
        verifierTaille(texte, 8192);
        String lien = url == null ? "" : url.strip();
        if (lien.isEmpty()) {
            lien = extraire(texte == null ? "" : texte);
        }
        if (lien.isEmpty() || !UrlTitleService.estUrlHttpValide(lien)) {
            throw new RequeteInvalideException("Le partage ne contient pas de lien HTTP ou HTTPS valide.");
        }
        return new PartageLien(lien, nettoyerTitre(titre));
    }

    public static String nettoyerTitre(String titre) {
        String nom = ESPACES.matcher(titre == null ? "" : titre).replaceAll(" ").strip();
        int limite = Math.min(nom.length(), SouhaitService.NOM_MAX);
        if (limite < nom.length() && Character.isHighSurrogate(nom.charAt(limite - 1))) {
            limite--;
        }
        return nom.substring(0, limite);
    }

    private static void verifierTaille(String valeur, int maximum) {
        if (valeur != null && valeur.length() > maximum) {
            throw new RequeteInvalideException("Le contenu partage est trop long.");
        }
    }

    private static String extraire(String texte) {
        Matcher liens = LIEN.matcher(texte);
        while (liens.find()) {
            String candidat = liens.group();
            while (!candidat.isEmpty() && estDelimiteurFinal(candidat)) {
                candidat = candidat.substring(0, candidat.length() - 1);
            }
            if (candidat.length() <= 2048 && UrlTitleService.estUrlHttpValide(candidat)) {
                return candidat;
            }
        }
        return "";
    }

    private static boolean estDelimiteurFinal(String lien) {
        char dernier = lien.charAt(lien.length() - 1);
        if (dernier == '.' || dernier == ',' || dernier == ';' || dernier == '!') {
            return true;
        }
        char ouvrant = switch (dernier) {
            case ')' -> '(';
            case ']' -> '[';
            case '}' -> '{';
            default -> 0;
        };
        return ouvrant != 0 && lien.chars().filter(caractere -> caractere == dernier).count()
                > lien.chars().filter(caractere -> caractere == ouvrant).count();
    }
}