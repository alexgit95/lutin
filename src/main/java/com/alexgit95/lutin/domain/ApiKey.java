package com.alexgit95.lutin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

/** Clé d'API de sauvegarde : seule l'empreinte SHA-256 est conservée, jamais la clé en clair. */
@Entity
@Table(name = "api_key")
@Getter
@NoArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    // Nom en minuscules : unicité sans tenir compte de la casse.
    @Column(name = "nom_cle", nullable = false, unique = true, length = 100)
    private String nomCle;

    @Column(nullable = false, unique = true, length = 64)
    private String empreinte;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Setter
    @Column(name = "derniere_utilisation")
    private Instant derniereUtilisation;

    public ApiKey(String nom, String empreinte, Instant creeLe) {
        this.nom = nom;
        this.nomCle = cle(nom);
        this.empreinte = empreinte;
        this.creeLe = creeLe;
    }

    public static String cle(String nom) {
        return nom.trim().toLowerCase(Locale.ROOT);
    }
}
