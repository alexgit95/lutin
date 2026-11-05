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

@Entity
// Contrainte composite ignorée par le dialecte SQLite : l'unicité est aussi vérifiée par le service.
@Table(name = "tag", uniqueConstraints = @UniqueConstraint(columnNames = {"nom", "liste_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liste_id", nullable = false)
    private ListeAchat liste;

    public Tag(String nom, ListeAchat liste) {
        this.nom = nom;
        this.liste = liste;
    }
}
