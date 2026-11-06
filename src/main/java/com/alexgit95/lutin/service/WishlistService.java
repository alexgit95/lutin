package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WishlistService {

    public record WishlistResume(Long id, String nom, String proprietaire) {
    }

    private static final String MESSAGE_DOUBLON = "Vous avez déjà une wishlist de ce nom";

    private final WishlistRepository wishlists;
    private final UtilisateurRepository utilisateurs;
    private final SouhaitRepository souhaits;
    private final WishlistPartageRepository partages;
    private final AccesWishlistService acces;
    private final AvisService avis;

    public WishlistService(WishlistRepository wishlists, UtilisateurRepository utilisateurs,
                           SouhaitRepository souhaits, WishlistPartageRepository partages,
                           AccesWishlistService acces, AvisService avis) {
        this.wishlists = wishlists;
        this.utilisateurs = utilisateurs;
        this.souhaits = souhaits;
        this.partages = partages;
        this.acces = acces;
        this.avis = avis;
    }

    @Transactional(readOnly = true)
    public List<WishlistResume> mesWishlists(String login) {
        return wishlists.findByProprietaireLoginOrderByNom(login).stream()
                .map(w -> new WishlistResume(w.getId(), w.getNom(), login)).toList();
    }

    @Transactional(readOnly = true)
    public List<WishlistResume> wishlistsPartagees(String login) {
        return partages.findByLecteurLoginOrderByWishlistNom(login).stream()
                .map(p -> new WishlistResume(p.getWishlist().getId(), p.getWishlist().getNom(),
                        p.getWishlist().getProprietaire().getLogin())).toList();
    }

    @Transactional(readOnly = true)
    public String nom(Long wishlistId, String login) {
        return acces.proprietaire(wishlistId, login).getNom();
    }

    public Wishlist creer(String login, String nom) {
        Utilisateur proprietaire = utilisateurs.findByLogin(login)
                .orElseThrow(() -> new ElementIntrouvableException("Utilisateur inconnu"));
        String propre = nomValide(nom);
        // Le dialecte SQLite n'applique pas la contrainte composite : unicité vérifiée ici.
        if (wishlists.existsByProprietaireIdAndNomCle(proprietaire.getId(), Wishlist.cle(propre))) {
            throw new RequeteInvalideException(MESSAGE_DOUBLON);
        }
        return wishlists.save(new Wishlist(propre, proprietaire));
    }

    public void renommer(Long wishlistId, String login, String nom) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        String propre = nomValide(nom);
        if (wishlists.existsByProprietaireIdAndNomCleAndIdNot(wishlist.getProprietaire().getId(),
                Wishlist.cle(propre), wishlistId)) {
            throw new RequeteInvalideException(MESSAGE_DOUBLON);
        }
        wishlist.renommer(propre);
    }

    public void supprimer(Long wishlistId, String login) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        avis.wishlistSupprimee(wishlist, souhaits.countByWishlistIdAndPrisParNomIsNotNull(wishlistId));
        souhaits.supprimerDeLaWishlist(wishlistId);
        partages.supprimerDeLaWishlist(wishlistId);
        wishlists.delete(wishlist);
    }

    private String nomValide(String nom) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty() || propre.length() > 100) {
            throw new RequeteInvalideException("Le nom de la wishlist est obligatoire (100 caractères maximum)");
        }
        return propre;
    }
}
