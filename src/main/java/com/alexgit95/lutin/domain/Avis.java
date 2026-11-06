package com.alexgit95.lutin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Avis destiné à un lecteur : instantané sans clé vers l'article ou la wishlist, qui peuvent disparaître. */
@Entity
@Table(name = "avis", indexes = @Index(name = "idx_avis_destinataire", columnList = "destinataire_id"))
@Getter
@Setter
@NoArgsConstructor
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeAvis type;

    @Column(name = "souhait_id")
    private Long souhaitId;

    @Column(name = "wishlist_id")
    private Long wishlistId;

    @Column(name = "wishlist_nom", nullable = false, length = 100)
    private String wishlistNom;

    @Column(name = "article_nom")
    private String articleNom;

    @Column(name = "pris_par_nom", length = 100)
    private String prisParNom;

    // Long et non Integer : une colonne « integer » placée avant l'id fait omettre son type par Hibernate sous SQLite.
    private Long nombre;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;
}
