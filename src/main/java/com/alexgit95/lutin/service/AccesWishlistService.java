package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Point unique des contrôles de droits sur une wishlist. L'administrateur n'a aucun accès particulier. */
@Service
@Transactional(readOnly = true)
public class AccesWishlistService {

    public record Acces(Wishlist wishlist, boolean proprietaire) {
    }

    private final WishlistRepository wishlists;
    private final WishlistPartageRepository partages;

    public AccesWishlistService(WishlistRepository wishlists, WishlistPartageRepository partages) {
        this.wishlists = wishlists;
        this.partages = partages;
    }

    public Wishlist proprietaire(Long wishlistId, String login) {
        Wishlist wishlist = charger(wishlistId);
        if (!wishlist.getProprietaire().getLogin().equals(login)) {
            throw new AccessDeniedException("Wishlist réservée à son propriétaire");
        }
        return wishlist;
    }

    /** Réservé aux destinataires du partage : le propriétaire est refusé. */
    public Wishlist lecteur(Long wishlistId, String login) {
        Wishlist wishlist = charger(wishlistId);
        if (wishlist.getProprietaire().getLogin().equals(login)
                || !partages.existsByWishlistIdAndLecteurLogin(wishlistId, login)) {
            throw new AccessDeniedException("Wishlist non accessible en tant que lecteur");
        }
        return wishlist;
    }

    public Acces consultation(Long wishlistId, String login) {
        Wishlist wishlist = charger(wishlistId);
        if (wishlist.getProprietaire().getLogin().equals(login)) {
            return new Acces(wishlist, true);
        }
        if (partages.existsByWishlistIdAndLecteurLogin(wishlistId, login)) {
            return new Acces(wishlist, false);
        }
        throw new AccessDeniedException("Wishlist non accessible");
    }

    // Une wishlist inexistante donne le même refus qu'une wishlist non accessible.
    private Wishlist charger(Long wishlistId) {
        return wishlists.findWithProprietaireById(wishlistId)
                .orElseThrow(() -> new AccessDeniedException("Wishlist non accessible"));
    }
}
