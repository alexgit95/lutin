package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import com.alexgit95.lutin.service.WishlistPubliqueService.Profil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class WishlistPubliqueTest {

    @Autowired WishlistService wishlistService;
    @Autowired SouhaitService souhaitService;
    @Autowired WishlistPartageService partageService;
    @Autowired WishlistPubliqueService publique;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired WishlistRepository wishlists;
    @Autowired SouhaitRepository souhaits;
    @Autowired WishlistPartageRepository partages;
    @Autowired AvisRepository avis;
    @MockitoBean UrlTitleService titres;

    private Long wishlistId;
    private Long souhaitId;
    private String jeton;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol", "dave"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        wishlistId = wishlistService.creer("alice", "Anniversaire").getId();
        partageService.partager(wishlistId, "alice", "bob");
        souhaitId = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Casque", null, null)).getId();
        jeton = wishlistService.rendrePublique(wishlistId, "alice");
    }

    @AfterEach
    void nettoyer() {
        avis.deleteAll();
        souhaits.deleteAll();
        partages.deleteAll();
        wishlists.deleteAll();
        utilisateurs.deleteAll();
    }

    private String preneurVu() {
        return publique.consulter(jeton, null, 0).vue().souhaits().get(0).prisPar();
    }

    // --- Cycle du jeton ---

    @Test
    void leJetonEstAleatoireUrlSafeEtIdempotent() {
        assertThat(jeton).matches("[A-Za-z0-9_-]{22}");
        assertThat(wishlistService.rendrePublique(wishlistId, "alice")).isEqualTo(jeton);
        assertThat(WishlistService.nouveauJeton()).isNotEqualTo(WishlistService.nouveauJeton());
    }

    @Test
    void regenererInvalideLAncienLien() {
        String nouveau = wishlistService.regenererLien(wishlistId, "alice");

        assertThat(nouveau).isNotEqualTo(jeton);
        assertThatThrownBy(() -> publique.consulter(jeton, null, 0)).isInstanceOf(ElementIntrouvableException.class);
        assertThat(publique.consulter(nouveau, null, 0).vue().nom()).isEqualTo("Anniversaire");
    }

    @Test
    void rendrePriveeRevoqueTousLesLiensEtGardeLesArticlesPris() {
        publique.prendre(jeton, null, souhaitId, "Tante Marie");

        wishlistService.rendrePrivee(wishlistId, "alice");

        assertThat(wishlistService.jetonPublic(wishlistId, "alice")).isNull();
        assertThatThrownBy(() -> publique.consulter(jeton, null, 0)).isInstanceOf(ElementIntrouvableException.class);
        assertThat(souhaitService.vueLecteur(wishlistId, "bob", 0).souhaits().get(0).prisPar())
                .isEqualTo("Tante Marie (invité)");
    }

    @Test
    void regenererUneWishlistPriveeEstRefuse() {
        wishlistService.rendrePrivee(wishlistId, "alice");

        assertThatThrownBy(() -> wishlistService.regenererLien(wishlistId, "alice"))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void seulLeProprietaireGereLeLien() {
        for (String intrus : new String[]{"bob", "dave"}) {
            assertThatThrownBy(() -> wishlistService.rendrePublique(wishlistId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wishlistService.regenererLien(wishlistId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wishlistService.rendrePrivee(wishlistId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wishlistService.jetonPublic(wishlistId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // --- Accès par le lien ---

    @Test
    void unJetonNulVideOuInconnuNeRetrouveJamaisUneWishlist() {
        Long privee = wishlistService.creer("alice", "Privee").getId();
        assertThat(wishlistService.jetonPublic(privee, "alice")).isNull();

        for (String mauvais : new String[]{null, "", "  ", "inconnu"}) {
            assertThatThrownBy(() -> publique.consulter(mauvais, null, 0))
                    .isInstanceOf(ElementIntrouvableException.class);
        }
    }

    @Test
    void laVuePubliqueNeContientAucunIdentifiantDuProprietaire() {
        var consultation = publique.consulter(jeton, null, 0);

        assertThat(consultation.profil()).isEqualTo(Profil.INVITE);
        assertThat(consultation.vue().proprietaire()).isNull();
        assertThat(consultation.vue().nom()).isEqualTo("Anniversaire");
        assertThat(consultation.vue().souhaits()).hasSize(1);
    }

    @Test
    void lesProfilsSontRedirigesOuServisSelonLeVisiteur() {
        var proprietaire = publique.consulter(jeton, "alice", 0);
        var partage = publique.consulter(jeton, "bob", 0);
        var compte = publique.consulter(jeton, "dave", 0);

        assertThat(proprietaire.profil()).isEqualTo(Profil.PROPRIETAIRE);
        assertThat(proprietaire.vue()).isNull();
        assertThat(proprietaire.wishlistId()).isEqualTo(wishlistId);
        assertThat(partage.profil()).isEqualTo(Profil.LECTEUR_PARTAGE);
        assertThat(partage.vue()).isNull();
        assertThat(compte.profil()).isEqualTo(Profil.COMPTE);
        assertThat(compte.vue()).isNotNull();
    }

    // --- Prise ---

    @Test
    void unInvitePrendSousSonNomAvecLaMentionInvite() {
        publique.prendre(jeton, null, souhaitId, "  Tante   Marie ");

        assertThat(preneurVu()).isEqualTo("Tante Marie (invité)");
        assertThat(souhaitService.vueLecteur(wishlistId, "bob", 0).souhaits().get(0).prisPar())
                .isEqualTo("Tante Marie (invité)");
    }

    @Test
    void lesNomsInvalidesSontRefusesEtLArticleResteLibre() {
        for (String mauvais : new String[]{null, "", "   ", "\n\t", "x".repeat(101)}) {
            assertThatThrownBy(() -> publique.prendre(jeton, null, souhaitId, mauvais))
                    .isInstanceOf(RequeteInvalideException.class);
        }
        assertThat(preneurVu()).isNull();
        publique.prendre(jeton, null, souhaitId, "x".repeat(100));
        assertThat(preneurVu()).isEqualTo("x".repeat(100) + " (invité)");
    }

    @Test
    void leNomEstNettoyeDesCaracteresDeControleMaisPasDuHtml() {
        assertThat(WishlistPubliqueService.nomInvite("Ma\u0000rie\u202E\nCurie")).isEqualTo("Ma rie Curie");
        assertThat(WishlistPubliqueService.nomInvite("<b>X</b>")).isEqualTo("<b>X</b>");
    }

    @Test
    void unCompteConnectePrendSousSonLoginSansMentionInvite() {
        publique.prendre(jeton, "dave", souhaitId, "Ignore");

        assertThat(preneurVu()).isEqualTo("dave");
    }

    @Test
    void leProprietaireNePeutNiPrendreNiLibererParLeLien() {
        assertThatThrownBy(() -> publique.prendre(jeton, "alice", souhaitId, "Moi"))
                .isInstanceOf(AccessDeniedException.class);
        publique.prendre(jeton, null, souhaitId, "Marie");
        assertThatThrownBy(() -> publique.liberer(jeton, "alice", souhaitId, true))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(preneurVu()).isEqualTo("Marie (invité)");
    }

    @Test
    void laPriseConcurrenteNommeLePreneurAvecLaMention() {
        publique.prendre(jeton, null, souhaitId, "Marie");

        assertThatThrownBy(() -> publique.prendre(jeton, "dave", souhaitId, null))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("Marie (invité)");
        assertThatThrownBy(() -> publique.prendre(jeton, null, souhaitId, "Paul"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThat(preneurVu()).isEqualTo("Marie (invité)");
    }

    @Test
    void unArticleDUneAutreWishlistEstIntrouvableParLeLien() {
        Long autre = wishlistService.creer("alice", "Autre").getId();
        Long articleAutre = souhaitService.creer(autre, "alice", new SouhaitCommande("Livre", null, null)).getId();

        assertThatThrownBy(() -> publique.prendre(jeton, null, articleAutre, "Marie"))
                .isInstanceOf(ElementIntrouvableException.class);
        assertThat(souhaitService.vueProprietaire(autre, "alice", 0).souhaits()).hasSize(1);
        assertThat(souhaits.findAll()).allSatisfy(s -> assertThat(s.getPrisParNom()).isNull());
        assertThatThrownBy(() -> publique.liberer(jeton, null, articleAutre, true))
                .isInstanceOf(ElementIntrouvableException.class);
    }

    // --- Libération ---

    @Test
    void unInviteDoitToujoursConfirmerLaLiberation() {
        publique.prendre(jeton, null, souhaitId, "Marie");

        var sans = publique.liberer(jeton, null, souhaitId, false);
        assertThat(sans.libere()).isFalse();
        assertThat(sans.preneur()).isEqualTo("Marie (invité)");
        assertThat(preneurVu()).isEqualTo("Marie (invité)");

        assertThat(publique.liberer(jeton, null, souhaitId, true).libere()).isTrue();
        assertThat(preneurVu()).isNull();
    }

    @Test
    void unCompteLiberePropreArticleSansAvertissement() {
        publique.prendre(jeton, "dave", souhaitId, null);

        assertThat(publique.liberer(jeton, "dave", souhaitId, false).libere()).isTrue();
        assertThat(preneurVu()).isNull();
    }

    @Test
    void unCompteDoitConfirmerPourLArticleDUnAutreOuDUnInviteHomonyme() {
        publique.prendre(jeton, null, souhaitId, "dave");

        var homonyme = publique.liberer(jeton, "dave", souhaitId, false);
        assertThat(homonyme.libere()).isFalse();
        assertThat(homonyme.preneur()).isEqualTo("dave (invité)");
        assertThat(publique.liberer(jeton, "carol", souhaitId, false).libere()).isFalse();
        assertThat(publique.liberer(jeton, "dave", souhaitId, true).libere()).isTrue();
    }

    @Test
    void libererUnArticleLibreNeFaitRien() {
        assertThat(publique.liberer(jeton, null, souhaitId, false).libere()).isTrue();
    }

    @Test
    void lesLecteursPartagesPeuventAussiPrendreParLeLienSousLeurLogin() {
        publique.prendre(jeton, "bob", souhaitId, null);

        assertThat(preneurVu()).isEqualTo("bob");
    }
}
