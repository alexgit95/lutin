package com.alexgit95.lutin.vue;

/** Clé d'API telle qu'affichée à l'administrateur : dates déjà formatées, {@code derniereUtilisation} nul si jamais utilisée. */
public record VueCleApi(Long id, String nom, String creeLe, String derniereUtilisation) {
}
