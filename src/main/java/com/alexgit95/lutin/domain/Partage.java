package com.alexgit95.lutin.domain;

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

@Entity
// Contrainte composite ignorée par le dialecte SQLite : l'unicité est aussi vérifiée par le service.
@Table(name = "partage", uniqueConstraints = @UniqueConstraint(columnNames = {"liste_id", "lecteur_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Partage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liste_id", nullable = false)
    private ListeAchat liste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecteur_id", nullable = false)
    private Utilisateur lecteur;

    public Partage(ListeAchat liste, Utilisateur lecteur) {
        this.liste = liste;
        this.lecteur = lecteur;
    }
}
