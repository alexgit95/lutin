package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Avis;
import com.alexgit95.lutin.domain.Souhait;
import com.alexgit95.lutin.domain.TypeAvis;
import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.domain.WishlistPartage;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Avis envoyés aux lecteurs connectés quand le propriétaire touche à un article pris ou supprime une wishlist. */
@Service
@Transactional
public class AvisService {

    public static final Duration VALIDITE = Duration.ofDays(90);
    public static final int TAILLE_PAGE = 20;

    public record VueAvis(Long id, TypeAvis type, Long wishlistId, String message) {
    }

    private final AvisRepository avis;
    private final WishlistPartageRepository partages;
    private final Clock horloge;

    public AvisService(AvisRepository avis, WishlistPartageRepository partages, Clock horloge) {
        this.avis = avis;
        this.partages = partages;
        this.horloge = horloge;
    }

    /** À appeler pour un article pris : un seul avis de modification par article et par lecteur. */
    public void articleModifie(Wishlist wishlist, Souhait souhait) {
        for (WishlistPartage partage : partages.findByWishlistIdOrderByLecteurLogin(wishlist.getId())) {
            if (!avis.existsByDestinataireIdAndSouhaitIdAndType(partage.getLecteur().getId(), souhait.getId(),
                    TypeAvis.MODIFIE)) {
                avis.save(nouveau(partage, TypeAvis.MODIFIE, wishlist, souhait, null));
            }
        }
    }

    /** À appeler pour un article pris, avant sa suppression : l'avis de suppression remplace ceux de modification. */
    public void articleSupprime(Wishlist wishlist, Souhait souhait) {
        List<WishlistPartage> lecteurs = partages.findByWishlistIdOrderByLecteurLogin(wishlist.getId());
        avis.supprimerDeLArticle(souhait.getId(), TypeAvis.MODIFIE);
        for (WishlistPartage partage : lecteurs) {
            avis.save(nouveau(partage, TypeAvis.SUPPRIME, wishlist, souhait, null));
        }
    }

    /** À appeler avant la suppression des partages. Sans article pris, les avis déjà reçus sont conservés. */
    public void wishlistSupprimee(Wishlist wishlist, long nombrePris) {
        List<WishlistPartage> lecteurs = partages.findByWishlistIdOrderByLecteurLogin(wishlist.getId());
        if (nombrePris == 0) {
            avis.detacherDeLaWishlist(wishlist.getId());
            return;
        }
        avis.supprimerDeLaWishlist(wishlist.getId());
        for (WishlistPartage partage : lecteurs) {
            Avis a = nouveau(partage, TypeAvis.WISHLIST_SUPPRIMEE, wishlist, null, nombrePris);
            a.setWishlistId(null);
            avis.save(a);
        }
    }

    public void articleLibere(Long souhaitId) {
        avis.supprimerDeLArticle(souhaitId, TypeAvis.MODIFIE);
    }

    public void lecteurRetire(Long lecteurId, Long wishlistId) {
        avis.supprimerDuLecteur(lecteurId, wishlistId);
    }

    @Transactional(readOnly = true)
    public Page<VueAvis> lister(String login, int page) {
        return avis.findByDestinataireLoginAndCreeLeAfterOrderByCreeLeDescIdDesc(login, limite(),
                PageRequest.of(Math.max(page, 0), TAILLE_PAGE)).map(AvisService::vue);
    }

    @Transactional(readOnly = true)
    public List<VueAvis> pourWishlist(String login, Long wishlistId) {
        return avis.findByDestinataireLoginAndWishlistIdAndCreeLeAfterOrderByCreeLeDescIdDesc(login, wishlistId,
                limite(), PageRequest.of(0, TAILLE_PAGE)).map(AvisService::vue).getContent();
    }

    @Transactional(readOnly = true)
    public long compter(String login) {
        return avis.countByDestinataireLoginAndCreeLeAfter(login, limite());
    }

    /** Fermeture définitive ; un avis absent ou d'un autre utilisateur est ignoré. */
    public void fermer(String login, Long avisId) {
        avis.findByIdAndDestinataireLogin(avisId, login).ifPresent(avis::delete);
    }

    public void purgerExpires() {
        avis.supprimerAvant(limite());
    }

    private Instant limite() {
        return horloge.instant().minus(VALIDITE);
    }

    private Avis nouveau(WishlistPartage partage, TypeAvis type, Wishlist wishlist, Souhait souhait, Long nombre) {
        Avis a = new Avis();
        a.setDestinataire(partage.getLecteur());
        a.setType(type);
        a.setWishlistId(wishlist.getId());
        a.setWishlistNom(wishlist.getNom());
        if (souhait != null) {
            a.setSouhaitId(souhait.getId());
            a.setArticleNom(souhait.getNom());
            a.setPrisParNom(souhait.getPrisParNom());
        }
        a.setNombre(nombre);
        a.setCreeLe(horloge.instant());
        return a;
    }

    static VueAvis vue(Avis a) {
        String message = switch (a.getType()) {
            case MODIFIE -> "L'article « " + a.getArticleNom() + " » de la wishlist « " + a.getWishlistNom()
                    + " », pris par " + a.getPrisParNom() + ", a été modifié.";
            case SUPPRIME -> "L'article « " + a.getArticleNom() + " » de la wishlist « " + a.getWishlistNom()
                    + " », pris par " + a.getPrisParNom() + ", a été supprimé.";
            case WISHLIST_SUPPRIMEE -> "La wishlist « " + a.getWishlistNom() + " » a été supprimée ("
                    + a.getNombre() + (a.getNombre() > 1 ? " articles pris)." : " article pris).");
        };
        return new VueAvis(a.getId(), a.getType(), a.getWishlistId(), message);
    }
}
