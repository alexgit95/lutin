package com.alexgit95.lutin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cadeau")
@Getter
@Setter
@NoArgsConstructor
public class Cadeau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 2048)
    private String url;

    @Column(precision = 12, scale = 2)
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.A_ACHETER;

    @Column(nullable = false)
    private boolean masque;

    @Column(name = "pour_tiers", nullable = false)
    private boolean pourTiers;

    // Facultative : nul = non remboursé. Jamais exposée à un lecteur.
    @Column(name = "rembourse_le")
    private LocalDate rembourseLe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personne_id", nullable = false)
    private Personne personne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liste_id", nullable = false)
    private ListeAchat liste;

    @ManyToMany
    @BatchSize(size = 100)
    @JoinTable(name = "cadeau_tag",
            joinColumns = @JoinColumn(name = "cadeau_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    /** Même règle que « Déjà déboursé » : l'argent a quitté le compte courant. */
    public boolean estRemboursable() {
        return !pourTiers && prix != null && statut != Statut.A_ACHETER;
    }
}
