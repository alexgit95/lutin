package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Avis;
import com.alexgit95.lutin.domain.TypeAvis;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import com.alexgit95.lutin.vue.VueSouhaitLecteur;
import com.alexgit95.lutin.vue.VueSouhaitProprietaire;
import com.alexgit95.lutin.vue.VueWishlistLecteur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class WishlistsParcoursTest {

    @Autowired WishlistService wishlistService;
    @Autowired SouhaitService souhaitService;
    @Autowired WishlistPartageService partageService;
    @Autowired AvisService avisService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired WishlistRepository wishlists;
    @Autowired SouhaitRepository souhaits;
    @Autowired WishlistPartageRepository partages;
    @Autowired AvisRepository avis;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean UrlTitleService titres;

    private Long wishlistId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol", "dave"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        wishlistId = wishlistService.creer("alice", "Anniversaire").getId();
        partageService.partager(wishlistId, "alice", "bob");
        partageService.partager(wishlistId, "alice", "carol");
    }

    @AfterEach
    void nettoyer() {
        avis.deleteAll();
        souhaits.deleteAll();
        partages.deleteAll();
        wishlists.deleteAll();
        utilisateurs.deleteAll();
    }

    private Long article(String nom) {
        return souhaitService.creer(wishlistId, "alice", new SouhaitCommande(nom, null, null)).getId();
    }

    private List<Avis> avisDe(String login) {
        return new TransactionTemplate(transactions).execute(statut -> avis.findAll().stream()
                .filter(a -> a.getDestinataire().getLogin().equals(login)).toList());
    }

    // --- Wishlists ---

    @Test
    void lesNomsVidesSontRefuses() {
        assertThatThrownBy(() -> wishlistService.creer("alice", "  ")).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> wishlistService.creer("alice", null)).isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void unProprietaireNePeutAvoirDeuxWishlistsDeMemeNomSansTenirCompteDeLaCasseNiDesEspaces() {
        assertThatThrownBy(() -> wishlistService.creer("alice", "  ANNIVERSAIRE "))
                .isInstanceOf(RequeteInvalideException.class);
        wishlistService.creer("alice", "Noel");
        assertThatThrownBy(() -> wishlistService.renommer(wishlistId, "alice", "noel"))
                .isInstanceOf(RequeteInvalideException.class);
        wishlistService.renommer(wishlistId, "alice", "ANNIVERSAIRE");
        assertThat(wishlistService.nom(wishlistId, "alice")).isEqualTo("ANNIVERSAIRE");
    }

    @Test
    void deuxProprietairesPeuventNommerPareil() {
        wishlistService.creer("bob", "Anniversaire");
        assertThat(wishlistService.mesWishlists("bob")).hasSize(1);
    }

    @Test
    void lesWishlistsPartageesSontListeesSeparement() {
        assertThat(wishlistService.mesWishlists("alice")).hasSize(1);
        assertThat(wishlistService.mesWishlists("bob")).isEmpty();
        assertThat(wishlistService.wishlistsPartagees("bob")).extracting(WishlistService.WishlistResume::proprietaire)
                .containsExactly("alice");
    }

    @Test
    void laSuppressionEmporteArticlesEtPartages() {
        article("Livre");
        wishlistService.supprimer(wishlistId, "alice");

        assertThat(wishlists.findAll()).isEmpty();
        assertThat(souhaits.findAll()).isEmpty();
        assertThat(partages.findAll()).isEmpty();
    }

    @Test
    void seulLeProprietairePeutGererEtLAdminNAAucunAccesParticulier() {
        assertThatThrownBy(() -> wishlistService.supprimer(wishlistId, "bob")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> souhaitService.vueProprietaire(wishlistId, "bob", 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> souhaitService.vueLecteur(wishlistId, "dave", 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> souhaitService.vueLecteur(wishlistId, "alice", 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> souhaitService.vueProprietaire(9999L, "alice", 0))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Articles ---

    @Test
    void unArticleSeCreeAvecLeNomSeul() {
        Long id = article("Livre");
        SouhaitCommande c = souhaitService.obtenir(wishlistId, id, "alice");
        assertThat(c.url()).isNull();
        assertThat(c.prix()).isNull();
    }

    @Test
    void lesArticlesInvalidesSontRefuses() {
        assertThatThrownBy(() -> souhaitService.creer(wishlistId, "alice", new SouhaitCommande(" ", null, null)))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("X", "javascript:alert(1)", null))).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("X", null, new BigDecimal("-1")))).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("x".repeat(256), null, null))).isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void leTitreDeLUrlCompleteLeNomVide() {
        when(titres.titre("https://shop.example/a")).thenReturn(Optional.of("Super titre"));

        SouhaitCommande complete = souhaitService.completerNom(new SouhaitCommande("", "https://shop.example/a", null));
        assertThat(complete.nom()).isEqualTo("Super titre");
        assertThat(souhaitService.completerNom(new SouhaitCommande("Mien", "https://shop.example/a", null)).nom())
                .isEqualTo("Mien");
    }

    @Test
    void lePrixEstArrondiEtLeNomNettoye() {
        Long id = souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande(" Livre ", "https://shop.example/a", new BigDecimal("12.345"))).getId();
        SouhaitCommande c = souhaitService.obtenir(wishlistId, id, "alice");
        assertThat(c.nom()).isEqualTo("Livre");
        assertThat(c.prix()).isEqualByComparingTo("12.35");
    }

    @Test
    void lesArticlesAutresQueLesSiensSontIntrouvables() {
        Long autre = wishlistService.creer("alice", "Autre").getId();
        Long id = article("Livre");
        assertThatThrownBy(() -> souhaitService.modifier(autre, id, "alice", new SouhaitCommande("X", null, null)))
                .isInstanceOf(ElementIntrouvableException.class);
    }

    @Test
    void lOrdreEstStableEtLaVueDuProprietaireNeContientRienSurLEtatPris() {
        Long premier = article("A");
        article("B");
        souhaitService.prendre(wishlistId, premier, "bob");

        assertThat(souhaitService.vueProprietaire(wishlistId, "alice", 0).souhaits())
                .extracting(VueSouhaitProprietaire::nom).containsExactly("A", "B");
        assertThat(souhaitService.vueLecteur(wishlistId, "carol", 0).souhaits())
                .extracting(VueSouhaitLecteur::nom).containsExactly("A", "B");
        assertThat(Arrays.stream(VueSouhaitProprietaire.class.getRecordComponents()).map(RecordComponent::getName))
                .noneMatch(n -> n.toLowerCase().contains("pris"));
    }

    @Test
    void lesTuilesPortentUneInitialeEtLHoteSansWww() {
        souhaitService.creer(wishlistId, "alice", new SouhaitCommande("écharpe", "https://www.shop.example/a", null));
        article("🎁 Surprise");
        article("Sans lien");

        var souhaitsVus = souhaitService.vueProprietaire(wishlistId, "alice", 0).souhaits();

        assertThat(souhaitsVus.get(0).initiale()).isEqualTo("É");
        assertThat(souhaitsVus.get(0).hote()).isEqualTo("shop.example");
        assertThat(souhaitsVus.get(1).initiale()).isEqualTo("🎁");
        assertThat(souhaitsVus.get(2).hote()).isNull();
    }

    @Test
    void lesArticlesSontPagines() {
        for (int i = 0; i < SouhaitService.TAILLE_PAGE + 1; i++) {
            article("Article " + i);
        }
        var page0 = souhaitService.vueProprietaire(wishlistId, "alice", 0);
        var page1 = souhaitService.vueProprietaire(wishlistId, "alice", 1);
        assertThat(page0.souhaits()).hasSize(SouhaitService.TAILLE_PAGE);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page1.souhaits()).hasSize(1);
    }

    // --- Partage ---

    @Test
    void lesPartagesImpossiblesSontRefuses() {
        assertThatThrownBy(() -> partageService.partager(wishlistId, "alice", "alice"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> partageService.partager(wishlistId, "alice", "bob"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> partageService.partager(wishlistId, "alice", "inconnu"))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void seulLeProprietaireVoitEtGereLesPartages() {
        assertThat(partageService.candidats(wishlistId, "alice")).containsExactly("dave");
        assertThatThrownBy(() -> partageService.lister(wishlistId, "bob")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> partageService.candidats(wishlistId, "dave")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void leRetraitSupprimeLAccesEtLesAvisMaisLaisseLesArticlesPris() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");
        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v2", null, null));
        assertThat(avisDe("bob")).hasSize(1);

        Long partageBob = partageService.lister(wishlistId, "alice").stream()
                .filter(p -> p.lecteur().equals("bob")).findFirst().orElseThrow().id();
        partageService.retirer(wishlistId, partageBob, "alice");

        assertThat(avisDe("bob")).isEmpty();
        assertThat(avisDe("carol")).hasSize(1);
        assertThatThrownBy(() -> souhaitService.vueLecteur(wishlistId, "bob", 0))
                .isInstanceOf(AccessDeniedException.class);
        VueWishlistLecteur vue = souhaitService.vueLecteur(wishlistId, "carol", 0);
        assertThat(vue.souhaits().get(0).prisPar()).isEqualTo("bob");
    }

    // --- Prise et décochage ---

    @Test
    void unLecteurPrendUnArticleSousSonLogin() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");

        assertThat(souhaitService.vueLecteur(wishlistId, "carol", 0).souhaits().get(0).prisPar()).isEqualTo("bob");
    }

    @Test
    void unArticleDejaPrisNePeutPasEtrePrisUneSecondeFois() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");

        assertThatThrownBy(() -> souhaitService.prendre(wishlistId, id, "carol"))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("bob");
        assertThat(souhaitService.vueLecteur(wishlistId, "carol", 0).souhaits().get(0).prisPar()).isEqualTo("bob");
    }

    @Test
    void leProprietaireEtLesNonDestinatairesNePeuventNiPrendreNiLiberer() {
        Long id = article("Livre");
        assertThatThrownBy(() -> souhaitService.prendre(wishlistId, id, "alice")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> souhaitService.prendre(wishlistId, id, "dave")).isInstanceOf(AccessDeniedException.class);
        souhaitService.prendre(wishlistId, id, "bob");
        assertThatThrownBy(() -> souhaitService.liberer(wishlistId, id, "alice", true))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> souhaitService.liberer(wishlistId, id, "dave", true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void lePreneurLiberePropreArticleSansAvertissement() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");

        SouhaitService.ResultatLiberation r = souhaitService.liberer(wishlistId, id, "bob", false);

        assertThat(r.libere()).isTrue();
        assertThat(souhaitService.vueLecteur(wishlistId, "bob", 0).souhaits().get(0).prisPar()).isNull();
    }

    @Test
    void libererLArticleDUnAutreExigeUneConfirmation() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");

        SouhaitService.ResultatLiberation sans = souhaitService.liberer(wishlistId, id, "carol", false);
        assertThat(sans.libere()).isFalse();
        assertThat(sans.preneur()).isEqualTo("bob");
        assertThat(souhaitService.vueLecteur(wishlistId, "carol", 0).souhaits().get(0).prisPar()).isEqualTo("bob");

        assertThat(souhaitService.liberer(wishlistId, id, "carol", true).libere()).isTrue();
        assertThat(souhaitService.vueLecteur(wishlistId, "carol", 0).souhaits().get(0).prisPar()).isNull();
    }

    @Test
    void unLecteurNonIdentifieDoitToujoursConfirmer() {
        assertThat(SouhaitService.confirmationRequise("bob", false, null)).isTrue();
        assertThat(SouhaitService.confirmationRequise("bob", false, "carol")).isTrue();
        assertThat(SouhaitService.confirmationRequise("bob", false, "bob")).isFalse();
        assertThat(SouhaitService.confirmationRequise("bob", true, "bob")).isTrue();
    }

    @Test
    void libererUnArticleLibreNeFaitRien() {
        Long id = article("Livre");
        assertThat(souhaitService.liberer(wishlistId, id, "bob", false).libere()).isTrue();
    }

    @Test
    void libererPurgeLesAvisDeModification() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");
        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v2", null, null));
        assertThat(avis.findAll()).hasSize(2);

        souhaitService.liberer(wishlistId, id, "bob", false);

        assertThat(avis.findAll()).isEmpty();
    }

    // --- Avis ---

    @Test
    void aucunAvisPourUnArticleLibre() {
        Long id = article("Livre");
        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v2", null, null));
        souhaitService.supprimer(wishlistId, id, "alice");

        assertThat(avis.findAll()).isEmpty();
    }

    @Test
    void modifierUnArticlePrisPrevientLesLecteursSansDoublonEtGardeLePris() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");

        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v2", null, null));
        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v3", null, null));

        assertThat(avisDe("bob")).hasSize(1);
        assertThat(avisDe("carol")).hasSize(1);
        assertThat(avisDe("alice")).isEmpty();
        Avis a = avisDe("carol").get(0);
        assertThat(a.getType()).isEqualTo(TypeAvis.MODIFIE);
        assertThat(a.getPrisParNom()).isEqualTo("bob");
        assertThat(souhaitService.vueLecteur(wishlistId, "carol", 0).souhaits().get(0).prisPar()).isEqualTo("bob");
    }

    @Test
    void uneModificationSansChangementNeGenereAucunAvis() {
        Long id = souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("Livre", "https://shop.example/a", new BigDecimal("10"))).getId();
        souhaitService.prendre(wishlistId, id, "bob");

        souhaitService.modifier(wishlistId, id, "alice",
                new SouhaitCommande("Livre", "https://shop.example/a", new BigDecimal("10.00")));

        assertThat(avis.findAll()).isEmpty();
    }

    @Test
    void modifierLePrixDUnArticlePrisGenereUnAvis() {
        Long id = souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("Livre", null, new BigDecimal("10"))).getId();
        souhaitService.prendre(wishlistId, id, "bob");

        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre", null, new BigDecimal("12")));

        assertThat(avisDe("carol")).hasSize(1);
    }

    @Test
    void supprimerUnArticlePrisRemplaceLesAvisDeModification() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");
        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v2", null, null));

        souhaitService.supprimer(wishlistId, id, "alice");

        assertThat(souhaits.findAll()).isEmpty();
        assertThat(avis.findAll()).hasSize(2).allSatisfy(a -> assertThat(a.getType()).isEqualTo(TypeAvis.SUPPRIME));
        assertThat(avisService.lister("bob", 0).getContent().get(0).message())
                .contains("Livre v2").contains("pris par bob").contains("supprimé");
    }

    @Test
    void supprimerUneWishlistAvecDesArticlesPrisEnvoieUnSeulAvisParLecteur() {
        Long premier = article("A");
        Long second = article("B");
        article("C");
        souhaitService.prendre(wishlistId, premier, "bob");
        souhaitService.prendre(wishlistId, second, "carol");
        souhaitService.modifier(wishlistId, premier, "alice", new SouhaitCommande("A2", null, null));

        wishlistService.supprimer(wishlistId, "alice");

        assertThat(avis.findAll()).hasSize(2).allSatisfy(a -> {
            assertThat(a.getType()).isEqualTo(TypeAvis.WISHLIST_SUPPRIMEE);
            assertThat(a.getNombre()).isEqualTo(2);
        });
        assertThat(avisDe("bob")).hasSize(1);
        assertThat(avisService.lister("carol", 0).getContent().get(0).message()).contains("2 articles pris");
    }

    @Test
    void supprimerUneWishlistSansArticlePrisNeGenereAucunAvis() {
        article("A");
        wishlistService.supprimer(wishlistId, "alice");

        assertThat(avis.findAll()).isEmpty();
    }

    @Test
    void lesAvisDArticlesSurvivantsSontConservesSiAucunArticleNEstPris() {
        Long id = article("A");
        souhaitService.prendre(wishlistId, id, "bob");
        souhaitService.supprimer(wishlistId, id, "alice");
        assertThat(avis.findAll()).hasSize(2);

        wishlistService.supprimer(wishlistId, "alice");

        assertThat(avis.findAll()).hasSize(2).allSatisfy(a -> assertThat(a.getWishlistId()).isNull());
    }

    @Test
    void unLecteurFermeSesAvisMaisPasceuxDesAutres() {
        Long id = article("Livre");
        souhaitService.prendre(wishlistId, id, "bob");
        souhaitService.modifier(wishlistId, id, "alice", new SouhaitCommande("Livre v2", null, null));
        Long avisBob = avisDe("bob").get(0).getId();

        avisService.fermer("carol", avisBob);
        assertThat(avis.findAll()).hasSize(2);

        avisService.fermer("bob", avisBob);
        assertThat(avis.findAll()).hasSize(1);
        assertThat(avisService.compter("bob")).isZero();
        assertThat(avisService.compter("carol")).isEqualTo(1);
    }

    @Test
    void lesAvisExpirentApresQuatreVingtDixJours() {
        Utilisateur bob = utilisateurs.findByLogin("bob").orElseThrow();
        avis.saveAndFlush(avisDate(bob, Instant.now().minus(91, ChronoUnit.DAYS)));
        avis.saveAndFlush(avisDate(bob, Instant.now().minus(89, ChronoUnit.DAYS)));

        assertThat(avisService.compter("bob")).isEqualTo(1);
        assertThat(avisService.lister("bob", 0).getTotalElements()).isEqualTo(1);

        avisService.purgerExpires();

        assertThat(avis.findAll()).hasSize(1);
    }

    @Test
    void lesAvisSontPagines() {
        Utilisateur bob = utilisateurs.findByLogin("bob").orElseThrow();
        for (int i = 0; i < AvisService.TAILLE_PAGE + 1; i++) {
            avis.save(avisDate(bob, Instant.now()));
        }
        assertThat(avisService.lister("bob", 0).getContent()).hasSize(AvisService.TAILLE_PAGE);
        assertThat(avisService.lister("bob", 1).getContent()).hasSize(1);
    }

    private Avis avisDate(Utilisateur destinataire, Instant creeLe) {
        Avis a = new Avis();
        a.setDestinataire(destinataire);
        a.setType(TypeAvis.SUPPRIME);
        a.setWishlistNom("W");
        a.setArticleNom("Article");
        a.setPrisParNom("carol");
        a.setCreeLe(creeLe);
        return a;
    }
}
