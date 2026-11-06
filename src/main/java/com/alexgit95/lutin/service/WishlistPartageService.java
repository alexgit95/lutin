package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.domain.WishlistPartage;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class WishlistPartageService {

    public record VuePartage(Long id, String lecteur) {
    }

    private final WishlistPartageRepository partages;
    private final UtilisateurRepository utilisateurs;
    private final AccesWishlistService acces;
    private final AvisService avis;

    public WishlistPartageService(WishlistPartageRepository partages, UtilisateurRepository utilisateurs,
                                  AccesWishlistService acces, AvisService avis) {
        this.partages = partages;
        this.utilisateurs = utilisateurs;
        this.acces = acces;
        this.avis = avis;
    }

    @Transactional(readOnly = true)
    public List<VuePartage> lister(Long wishlistId, String login) {
        acces.proprietaire(wishlistId, login);
        return partages.findByWishlistIdOrderByLecteurLogin(wishlistId).stream()
                .map(p -> new VuePartage(p.getId(), p.getLecteur().getLogin())).toList();
    }

    /** Logins proposables : tous les comptes sauf le propriétaire et les lecteurs déjà ajoutés. */
    @Transactional(readOnly = true)
    public List<String> candidats(Long wishlistId, String login) {
        acces.proprietaire(wishlistId, login);
        Set<String> exclus = new HashSet<>();
        exclus.add(login);
        partages.findByWishlistIdOrderByLecteurLogin(wishlistId)
                .forEach(p -> exclus.add(p.getLecteur().getLogin()));
        return utilisateurs.findAllByOrderByLoginAsc().stream()
                .map(Utilisateur::getLogin).filter(l -> !exclus.contains(l)).toList();
    }

    public void partager(Long wishlistId, String login, String lecteurLogin) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        Utilisateur lecteur = utilisateurs.findByLogin(lecteurLogin == null ? "" : lecteurLogin.trim())
                .orElseThrow(() -> new RequeteInvalideException("Utilisateur inconnu"));
        if (lecteur.getLogin().equals(login)) {
            throw new RequeteInvalideException("Vous ne pouvez pas partager une wishlist avec vous-même");
        }
        // Le dialecte SQLite n'applique pas la contrainte composite : unicité vérifiée ici.
        if (partages.existsByWishlistIdAndLecteurId(wishlistId, lecteur.getId())) {
            throw new RequeteInvalideException("Cette wishlist est déjà partagée avec cet utilisateur");
        }
        partages.save(new WishlistPartage(wishlist, lecteur));
    }

    /** Les articles que le lecteur avait pris restent « pris par » son nom. */
    public void retirer(Long wishlistId, Long partageId, String login) {
        acces.proprietaire(wishlistId, login);
        WishlistPartage partage = partages.findById(partageId)
                .orElseThrow(() -> new ElementIntrouvableException("Partage introuvable"));
        if (!partage.getWishlist().getId().equals(wishlistId)) {
            throw new ElementIntrouvableException("Partage introuvable");
        }
        Long lecteurId = partage.getLecteur().getId();
        partages.delete(partage);
        avis.lecteurRetire(lecteurId, wishlistId);
    }
}
