package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Souhait;
import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.vue.VueSouhaitLecteur;
import com.alexgit95.lutin.vue.VueSouhaitProprietaire;
import com.alexgit95.lutin.vue.VueWishlistLecteur;
import com.alexgit95.lutin.vue.VueWishlistProprietaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class SouhaitService {

    /** Résultat d'un décochage : {@code preneur} n'est renseigné que si une confirmation est exigée. */
    public record ResultatLiberation(boolean libere, String preneur) {
    }

    public static final int TAILLE_PAGE = 50;
    static final int NOM_MAX = 100;
    private static final BigDecimal PRIX_MAX = new BigDecimal("10000000000");

    private final SouhaitRepository souhaits;
    private final AccesWishlistService acces;
    private final AvisService avis;
    private final UrlTitleService titres;
    private final Clock horloge;

    public SouhaitService(SouhaitRepository souhaits, AccesWishlistService acces, AvisService avis,
                          UrlTitleService titres, Clock horloge) {
        this.souhaits = souhaits;
        this.acces = acces;
        this.avis = avis;
        this.titres = titres;
        this.horloge = horloge;
    }

    /** Hors transaction : le réseau ne doit pas retenir une connexion à la base. */
    @Transactional(propagation = Propagation.SUPPORTS)
    public SouhaitCommande completerNom(SouhaitCommande commande) {
        boolean sansNom = commande.nom() == null || commande.nom().isBlank();
        String url = commande.url() == null ? "" : commande.url().trim();
        if (!sansNom || url.isEmpty() || !UrlTitleService.estUrlHttpValide(url)) {
            return commande;
        }
        return titres.titre(url).map(titre -> new SouhaitCommande(titre, commande.url(), commande.prix()))
                .orElse(commande);
    }

    @Transactional(readOnly = true)
    public VueWishlistProprietaire vueProprietaire(Long wishlistId, String login, int page) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        Page<Souhait> lot = lot(wishlistId, page);
        return new VueWishlistProprietaire(wishlist.getId(), wishlist.getNom(),
                lot.map(s -> new VueSouhaitProprietaire(s.getId(), s.getNom(), initiale(s.getNom()), s.getUrl(),
                        hote(s.getUrl()), s.getPrix())).getContent(),
                lot.getNumber(), lot.getTotalPages());
    }

    @Transactional(readOnly = true)
    public VueWishlistLecteur vueLecteur(Long wishlistId, String login, int page) {
        Wishlist wishlist = acces.lecteur(wishlistId, login);
        return vueLecteur(wishlist, wishlist.getProprietaire().getLogin(), page);
    }

    /** Vue du lien public : aucun identifiant du propriétaire. L'appelant a déjà résolu le jeton. */
    @Transactional(readOnly = true)
    public VueWishlistLecteur vuePublique(Wishlist wishlist, int page) {
        return vueLecteur(wishlist, null, page);
    }

    private VueWishlistLecteur vueLecteur(Wishlist wishlist, String proprietaire, int page) {
        Page<Souhait> lot = lot(wishlist.getId(), page);
        return new VueWishlistLecteur(wishlist.getId(), wishlist.getNom(), proprietaire,
                lot.map(s -> new VueSouhaitLecteur(s.getId(), s.getNom(), initiale(s.getNom()), s.getUrl(),
                        hote(s.getUrl()), s.getPrix(), s.nomPreneurAffiche())).getContent(),
                lot.getNumber(), lot.getTotalPages());
    }

    public Souhait creer(Long wishlistId, String login, SouhaitCommande commande) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        Souhait souhait = new Souhait();
        souhait.setWishlist(wishlist);
        appliquer(souhait, commande);
        return souhaits.save(souhait);
    }

    @Transactional(readOnly = true)
    public SouhaitCommande obtenir(Long wishlistId, Long souhaitId, String login) {
        acces.proprietaire(wishlistId, login);
        Souhait s = dansLaWishlist(wishlistId, souhaitId);
        return new SouhaitCommande(s.getNom(), s.getUrl(), s.getPrix());
    }

    public void modifier(Long wishlistId, Long souhaitId, String login, SouhaitCommande commande) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        Souhait souhait = dansLaWishlist(wishlistId, souhaitId);
        String nom = souhait.getNom();
        String url = souhait.getUrl();
        BigDecimal prix = souhait.getPrix();
        appliquer(souhait, commande);
        boolean change = !Objects.equals(nom, souhait.getNom()) || !Objects.equals(url, souhait.getUrl())
                || (prix == null ? souhait.getPrix() != null
                : souhait.getPrix() == null || prix.compareTo(souhait.getPrix()) != 0);
        if (change && souhait.getPrisParNom() != null) {
            avis.articleModifie(wishlist, souhait);
        }
    }

    public void supprimer(Long wishlistId, Long souhaitId, String login) {
        Wishlist wishlist = acces.proprietaire(wishlistId, login);
        Souhait souhait = dansLaWishlist(wishlistId, souhaitId);
        if (souhait.getPrisParNom() != null) {
            avis.articleSupprime(wishlist, souhait);
        }
        souhaits.delete(souhait);
    }

    /** Prise atomique : en cas de concurrence, un seul lecteur l'emporte. */
    public void prendre(Long wishlistId, Long souhaitId, String login) {
        acces.lecteur(wishlistId, login);
        prendreSansControle(wishlistId, souhaitId, login, false);
    }

    /** Prise au nom d'un tiers par un lecteur partagé : le login du lecteur est conservé comme auteur. */
    public void prendrePour(Long wishlistId, Long souhaitId, String login, String nomSaisi) {
        acces.lecteur(wishlistId, login);
        prendre(wishlistId, souhaitId, nomSaisi(nomSaisi), false, login);
    }

    /** Sans contrôle de droits : réservé à {@link WishlistPubliqueService}, qui a déjà identifié le visiteur. */
    public void prendreSansControle(Long wishlistId, Long souhaitId, String nom, boolean invite) {
        prendre(wishlistId, souhaitId, nom, invite, null);
    }

    private void prendre(Long wishlistId, Long souhaitId, String nom, boolean invite, String via) {
        if (souhaits.prendre(souhaitId, wishlistId, nom, horloge.instant(), invite, via) == 0) {
            Souhait existant = dansLaWishlist(wishlistId, souhaitId);
            throw new RequeteInvalideException(existant.getPrisParNom() == null
                    ? "Impossible de prendre cet article, réessayez"
                    : "Cet article est déjà pris par " + existant.nomPreneurAffiche());
        }
    }

    /** Sans confirmation, un article pris par quelqu'un d'autre n'est pas libéré. */
    public ResultatLiberation liberer(Long wishlistId, Long souhaitId, String login, boolean confirme) {
        acces.lecteur(wishlistId, login);
        return libererSansControle(wishlistId, souhaitId, login, confirme);
    }

    /** Sans contrôle de droits : réservé à {@link WishlistPubliqueService}. {@code login} nul pour un invité. */
    public ResultatLiberation libererSansControle(Long wishlistId, Long souhaitId, String login, boolean confirme) {
        Souhait souhait = dansLaWishlist(wishlistId, souhaitId);
        String preneur = souhait.getPrisParNom();
        if (preneur == null) {
            return new ResultatLiberation(true, null);
        }
        if (confirmationRequise(preneur, souhait.estPrisParInvite(), login, souhait.estPrisPourTiers()) && !confirme) {
            return new ResultatLiberation(false, souhait.nomPreneurAffiche());
        }
        souhaits.liberer(souhaitId, wishlistId);
        avis.articleLibere(souhaitId);
        return new ResultatLiberation(true, null);
    }

    /** Seul un compte connecté dont le login est le preneur, et non un invité, libère sans confirmation ; une prise pour un tiers aussi, pour tous. */
    public static boolean confirmationRequise(String preneur, boolean preneurInvite, String login,
                                              boolean prisPourTiers) {
        return !prisPourTiers && (preneurInvite || login == null || !login.equals(preneur));
    }

    /** Nom saisi par l'utilisateur pour un tiers ou un invité : nettoyé, obligatoire, borné. */
    static String nomSaisi(String saisi) {
        String propre = saisi == null ? "" : saisi.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").replaceAll(" +", " ").trim();
        if (propre.isEmpty()) {
            throw new RequeteInvalideException("Votre nom est obligatoire");
        }
        if (propre.length() > NOM_MAX) {
            throw new RequeteInvalideException("Le nom est trop long (" + NOM_MAX + " caractères maximum)");
        }
        return propre;
    }

    private Page<Souhait> lot(Long wishlistId, int page) {
        return souhaits.findByWishlistIdOrderByIdAsc(wishlistId, PageRequest.of(Math.max(page, 0), TAILLE_PAGE));
    }

    private static String initiale(String nom) {
        return new String(Character.toChars(nom.codePointAt(0))).toUpperCase(Locale.ROOT);
    }

    private static String hote(String url) {
        if (url == null) {
            return null;
        }
        try {
            String hote = URI.create(url).getHost();
            return hote != null && hote.startsWith("www.") ? hote.substring(4) : hote;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void appliquer(Souhait souhait, SouhaitCommande commande) {
        String url = commande.url() == null ? "" : commande.url().trim();
        if (url.length() > 2048 || (!url.isEmpty() && !UrlTitleService.estUrlHttpValide(url))) {
            throw new RequeteInvalideException("L'URL doit commencer par http:// ou https://");
        }
        String nom = commande.nom() == null ? "" : commande.nom().trim();
        if (nom.isEmpty()) {
            throw new RequeteInvalideException(url.isEmpty()
                    ? "Le nom est obligatoire"
                    : "Impossible de déduire le nom depuis l'URL, saisissez-le");
        }
        if (nom.length() > 255) {
            throw new RequeteInvalideException("Le nom est trop long (255 caractères maximum)");
        }
        souhait.setNom(nom);
        souhait.setUrl(url.isEmpty() ? null : url);
        souhait.setPrix(prixValide(commande.prix()));
    }

    private BigDecimal prixValide(BigDecimal prix) {
        if (prix == null) {
            return null;
        }
        BigDecimal arrondi = prix.setScale(2, RoundingMode.HALF_UP);
        if (arrondi.signum() < 0 || arrondi.compareTo(PRIX_MAX) >= 0) {
            throw new RequeteInvalideException("Le prix doit être positif");
        }
        return arrondi;
    }

    private Souhait dansLaWishlist(Long wishlistId, Long souhaitId) {
        return souhaits.findByIdAndWishlistId(souhaitId, wishlistId)
                .orElseThrow(() -> new ElementIntrouvableException("Article introuvable"));
    }
}
