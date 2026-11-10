package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import com.alexgit95.lutin.vue.VueWishlistLecteur;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Accès par lien public : le jeton ouvre la wishlist, le profil du visiteur décide de ce qu'il peut faire. */
@Service
@Transactional
public class WishlistPubliqueService {

    public enum Profil {
        PROPRIETAIRE, LECTEUR_PARTAGE, COMPTE, INVITE
    }

    /** {@code vue} n'est renseignée que pour les profils qui restent sur la vue publique. */
    public record Consultation(Profil profil, Long wishlistId, VueWishlistLecteur vue) {
    }

    private final WishlistRepository wishlists;
    private final WishlistPartageRepository partages;
    private final SouhaitService souhaits;

    public WishlistPubliqueService(WishlistRepository wishlists, WishlistPartageRepository partages,
                                   SouhaitService souhaits) {
        this.wishlists = wishlists;
        this.partages = partages;
        this.souhaits = souhaits;
    }

    /** {@code login} nul pour un visiteur non connecté. */
    @Transactional(readOnly = true)
    public Consultation consulter(String jeton, String login, int page) {
        Wishlist wishlist = resoudre(jeton);
        Profil profil = profil(wishlist, login);
        if (profil == Profil.PROPRIETAIRE || profil == Profil.LECTEUR_PARTAGE) {
            return new Consultation(profil, wishlist.getId(), null);
        }
        return new Consultation(profil, wishlist.getId(), souhaits.vuePublique(wishlist, page));
    }

    /** Un invité prend sous le nom saisi, un compte connecté sous son login. */
    public void prendre(String jeton, String login, Long souhaitId, String nomSaisi) {
        Wishlist wishlist = resoudre(jeton);
        Profil profil = profil(wishlist, login);
        if (profil == Profil.PROPRIETAIRE) {
            throw new AccessDeniedException("Le propriétaire ne peut pas prendre un article");
        }
        if (profil == Profil.INVITE) {
            souhaits.prendreSansControle(wishlist.getId(), souhaitId, nomInvite(nomSaisi), true);
        } else {
            souhaits.prendreSansControle(wishlist.getId(), souhaitId, login, false);
        }
    }

    public SouhaitService.ResultatLiberation liberer(String jeton, String login, Long souhaitId, boolean confirme) {
        Wishlist wishlist = resoudre(jeton);
        Profil profil = profil(wishlist, login);
        if (profil == Profil.PROPRIETAIRE) {
            throw new AccessDeniedException("Le propriétaire ne peut pas libérer un article");
        }
        return souhaits.libererSansControle(wishlist.getId(), souhaitId,
                profil == Profil.INVITE ? null : login, confirme);
    }

    // Un jeton nul ou vide ne doit jamais retrouver les wishlists privées (colonne nulle).
    private Wishlist resoudre(String jeton) {
        if (jeton == null || jeton.isBlank()) {
            throw new ElementIntrouvableException("Lien introuvable");
        }
        return wishlists.findWithProprietaireByJetonPublic(jeton)
                .orElseThrow(() -> new ElementIntrouvableException("Lien introuvable"));
    }

    private Profil profil(Wishlist wishlist, String login) {
        if (login == null) {
            return Profil.INVITE;
        }
        if (wishlist.getProprietaire().getLogin().equals(login)) {
            return Profil.PROPRIETAIRE;
        }
        return partages.existsByWishlistIdAndLecteurLogin(wishlist.getId(), login)
                ? Profil.LECTEUR_PARTAGE : Profil.COMPTE;
    }

    static String nomInvite(String saisi) {
        return SouhaitService.nomSaisi(saisi);
    }
}
