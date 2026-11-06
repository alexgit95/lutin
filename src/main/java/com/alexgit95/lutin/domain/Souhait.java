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
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.Instant;

/** Article d'une wishlist. L'état « pris » ne doit jamais être exposé au propriétaire. */
@Entity
@Table(name = "souhait")
// Une modification du propriétaire ne doit pas écraser un « pris » posé en parallèle.
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Souhait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 2048)
    private String url;

    @Column(precision = 12, scale = 2)
    private BigDecimal prix;

    // Login du preneur ou nom saisi : volontairement sans lien vers utilisateur.
    @Column(name = "pris_par_nom", length = 100)
    private String prisParNom;

    @Column(name = "pris_le")
    private Instant prisLe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false)
    private Wishlist wishlist;
}
