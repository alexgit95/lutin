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
@Table(name = "wishlist_partage", uniqueConstraints = @UniqueConstraint(columnNames = {"wishlist_id", "lecteur_id"}))
@Getter
@Setter
@NoArgsConstructor
public class WishlistPartage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecteur_id", nullable = false)
    private Utilisateur lecteur;

    public WishlistPartage(Wishlist wishlist, Utilisateur lecteur) {
        this.wishlist = wishlist;
        this.lecteur = lecteur;
    }
}
