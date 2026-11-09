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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Virement devenu inutile : un cadeau remboursé a cessé d'être remboursable. Reste jusqu'à sa fermeture. */
@Entity
@Table(name = "alerte_remboursement")
@Getter
@Setter
@NoArgsConstructor
public class AlerteRemboursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liste_id", nullable = false)
    private ListeAchat liste;

    @Column(name = "cadeau_nom", nullable = false)
    private String cadeauNom;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;
}
