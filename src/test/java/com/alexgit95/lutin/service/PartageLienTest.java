package com.alexgit95.lutin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartageLienTest {

    @Test
    void privilegieLUrlEtPreserveSonEncodage() {
        String url = "https://exemple.fr/a%20b?x=1&suite=%2F%3F+z#section";
        assertThat(PartageLien.depuis(" " + url + " ", "  Mon\n article\t\u0000 favori  ",
                "https://autre.fr")).isEqualTo(new PartageLien(url, "Mon article favori"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Voir\nhttps://exemple.fr/a?x=1&y=2#fin", "Voir <https://exemple.fr/a?x=1&y=2#fin>",
            "Voir (https://exemple.fr/a?x=1&y=2#fin).", "Voir \"https://exemple.fr/a?x=1&y=2#fin\""})
    void extraitUnLienDuTexte(String texte) {
        assertThat(PartageLien.depuis(null, null, texte))
                .isEqualTo(new PartageLien("https://exemple.fr/a?x=1&y=2#fin", ""));
    }

    @Test
    void retientLePremierLienValideEtPreserveLesParenthesesEquilibrees() {
        assertThat(PartageLien.depuis("  ", "", "https://user:pass@interdit.fr puis "
                + "(https://exemple.fr/article_(rouge)) puis https://autre.fr").url())
                .isEqualTo("https://exemple.fr/article_(rouge)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///secret", "ftp://exemple.fr", "monapp://objet/1",
            "https://u:p@exemple.fr", "https://", "pas une url", "https://exemple.fr/avec espace"})
    void refuseUneUrlExpliciteInvalideSansRepli(String url) {
        assertThatThrownBy(() -> PartageLien.depuis(url, "Nom", "https://valide.fr"))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "un article sans lien", "monapp://article/1"})
    void refuseUnPartageSansLien(String texte) {
        assertThatThrownBy(() -> PartageLien.depuis(null, null, texte))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void borneToutesLesEntreesAvantTraitement() {
        String url = "https://exemple.fr/";
        String urlMax = url + "a".repeat(2048 - url.length());
        assertThat(PartageLien.depuis(urlMax, "t".repeat(2048), "x".repeat(8192)).url()).isEqualTo(urlMax);
        assertThatThrownBy(() -> PartageLien.depuis(urlMax + "a", "", "https://valide.fr"))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("trop long");
        assertThatThrownBy(() -> PartageLien.depuis(url, "t".repeat(2049), ""))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> PartageLien.depuis(url, "", "x".repeat(8193)))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> PartageLien.depuis(null, null, urlMax + "a"))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void nettoieEtBorneLeTitreSansCouperUnePaireUnicode() {
        assertThat(PartageLien.nettoyerTitre(null)).isEmpty();
        assertThat(PartageLien.nettoyerTitre(" \t\n\u00a0\u200b ")).isEmpty();
        assertThat(PartageLien.nettoyerTitre("a".repeat(150))).isEqualTo("a".repeat(100));
        assertThat(PartageLien.nettoyerTitre("a".repeat(99) + "\ud83c\udf81")).isEqualTo("a".repeat(99));
        assertThat(PartageLien.nettoyerTitre("<script>alert(1)</script>"))
                .isEqualTo("<script>alert(1)</script>");
    }

    @Test
    void valideSansResolutionDnsNiAccesReseau() {
        assertThat(PartageLien.depuis("https://introuvable.invalid/objet", null, null).url())
                .isEqualTo("https://introuvable.invalid/objet");
    }
}