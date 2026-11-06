package com.alexgit95.lutin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Entity
// Contrainte composite ignorée par le dialecte SQLite : l'unicité est aussi vérifiée par le service.
@Table(name = "wishlist", uniqueConstraints = @UniqueConstraint(columnNames = {"proprietaire_id", "nom_cle"}))
@Getter
@NoArgsConstructor
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    // Nom nettoyé et en minuscules : sert à l'unicité sans tenir compte de la casse.
    @Column(name = "nom_cle", nullable = false, length = 100)
    private String nomCle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private Utilisateur proprietaire;

    public Wishlist(String nom, Utilisateur proprietaire) {
        this.proprietaire = proprietaire;
        renommer(nom);
    }

    public static String cle(String nom) {
        return nom.trim().toLowerCase(Locale.ROOT);
    }

    public void renommer(String nom) {
        this.nom = nom;
        this.nomCle = cle(nom);
    }
}
