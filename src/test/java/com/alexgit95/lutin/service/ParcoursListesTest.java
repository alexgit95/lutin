package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Tag;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.vue.VueCadeau;
import com.alexgit95.lutin.vue.VueListe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ParcoursListesTest {

    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired TagService tagsService;
    @Autowired PartageService partagesService;
    @Autowired DashboardService dashboard;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;
    @MockitoBean UrlTitleService titres;

    private Long listeId;
    private Long mamanId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        listeId = listesService.creer("alice", "Noel").getId();
        mamanId = personnesService.creer(listeId, "alice", "Maman", new BigDecimal("100")).getId();
    }

    @AfterEach
    void nettoyer() {
        cadeaux.deleteAll();
        partages.deleteAll();
        tags.deleteAll();
        personnes.deleteAll();
        listes.deleteAll();
        utilisateurs.deleteAll();
    }

    private Cadeau ajouter(String nom, String prix, Statut statut, boolean masque, boolean tiers) {
        CadeauCommande commande = new CadeauCommande(nom, null, prix == null ? null : new BigDecimal(prix),
                mamanId, masque, tiers, Set.of());
        Cadeau cadeau = cadeauxService.creer(listeId, "alice", commande);
        cadeauxService.changerStatut(listeId, cadeau.getId(), "alice", statut);
        return cadeau;
    }

    @Test
    void calculeLesTotauxDeLExempleDeLaSpec() {
        ajouter("A", "20", Statut.COMMANDE, false, false);
        ajouter("B", "30", Statut.A_ACHETER, false, false);
        ajouter("C", "50", Statut.COMMANDE, false, true);

        VueListe vue = dashboard.construire(listeId, "alice", null, 0);

        assertThat(vue.budgetGlobal()).isEqualByComparingTo("100");
        assertThat(vue.debourse()).isEqualByComparingTo("20");
        assertThat(vue.resteAPayer()).isEqualByComparingTo("30");
        assertThat(vue.totalCadeaux()).isEqualTo(3);
    }

    @Test
    void ignoreLesCadeauxSansPrixEtCompteLesStatutsAvances() {
        ajouter("sans prix", null, Statut.COMMANDE, false, false);
        ajouter("livre", "15", Statut.LIVRE, false, false);
        ajouter("remis", "5", Statut.REMIS, false, false);

        VueListe vue = dashboard.construire(listeId, "alice", null, 0);

        assertThat(vue.debourse()).isEqualByComparingTo("20");
        assertThat(vue.resteAPayer()).isEqualByComparingTo("0");
        assertThat(vue.repartition().get(Statut.COMMANDE)).isEqualTo(1);
        assertThat(vue.repartition().get(Statut.A_ACHETER)).isEqualTo(0);
    }

    @Test
    void lePrixPeutEtreRenseigneApresCoup() {
        Cadeau c = ajouter("Livre", null, Statut.A_ACHETER, false, false);
        cadeauxService.modifier(listeId, c.getId(), "alice",
                new CadeauCommande("Livre", null, new BigDecimal("12.5"), mamanId, false, false, Set.of()));

        assertThat(dashboard.construire(listeId, "alice", null, 0).resteAPayer()).isEqualByComparingTo("12.50");
    }

    @Test
    void unLecteurVoitUnCadeauMasqueSansDetailMaisDansLesTotaux() {
        partagesService.partager(listeId, "alice", "bob");
        Cadeau secret = ajouter("Secret", "40", Statut.COMMANDE, true, false);
        cadeauxService.modifier(listeId, secret.getId(), "alice", new CadeauCommande("Secret",
                "https://secret.example/x", new BigDecimal("40"), mamanId, true, false, Set.of()));
        ajouter("Visible", "10", Statut.A_ACHETER, false, false);

        VueListe lecteur = dashboard.construire(listeId, "bob", null, 0);

        VueCadeau masque = lecteur.cadeaux().stream().filter(c -> c.nom().equals("Secret")).findFirst().orElseThrow();
        assertThat(masque.id()).isNull();
        assertThat(masque.url()).isNull();
        assertThat(masque.prix()).isNull();
        assertThat(masque.statut()).isNull();
        assertThat(masque.personne()).isNull();
        assertThat(masque.personneId()).isNull();
        assertThat(masque.tags()).isEmpty();
        assertThat(masque.masque()).isTrue();
        assertThat(lecteur.debourse()).isEqualByComparingTo("40");
        assertThat(lecteur.resteAPayer()).isEqualByComparingTo("10");
        assertThat(lecteur.repartition().get(Statut.COMMANDE)).isEqualTo(0);
        assertThat(lecteur.lecteur()).isTrue();

            VueCadeau masqueDuLot = dashboard.chargerLot(listeId, "bob", null, 0).cadeaux().stream()
                .filter(c -> c.nom().equals("Secret")).findFirst().orElseThrow();
            assertThat(masqueDuLot.personneId()).isNull();
            assertThat(masqueDuLot.statut()).isNull();

        VueCadeau complet = dashboard.construire(listeId, "alice", null, 0).cadeaux().stream()
                .filter(c -> c.nom().equals("Secret")).findFirst().orElseThrow();
        assertThat(complet.url()).isEqualTo("https://secret.example/x");
        assertThat(complet.prix()).isEqualByComparingTo("40");
        assertThat(complet.personneId()).isEqualTo(mamanId);
        }

        @Test
        void distingueLesPersonnesHomonymesDansLaProjection() {
        Long homonymeId = personnesService.creer(listeId, "alice", "Maman", BigDecimal.TEN).getId();
        ajouter("A", null, Statut.A_ACHETER, false, false);
        cadeauxService.creer(listeId, "alice", new CadeauCommande("B", null, null,
            homonymeId, false, false, Set.of()));

        VueListe vue = dashboard.construire(listeId, "alice", null, 0);

        assertThat(vue.cadeaux()).extracting(VueCadeau::personneId)
            .containsExactlyInAnyOrder(mamanId, homonymeId);
        assertThat(vue.cadeaux()).extracting(VueCadeau::personne).containsOnly("Maman");
    }

    @Test
    void refuseLAccesAuxUtilisateursNonConcernes() {
        assertThatThrownBy(() -> dashboard.construire(listeId, "carol", null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> dashboard.construire(99999L, "alice", null, 0))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> dashboard.chargerLot(listeId, "carol", null, 0))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> dashboard.chargerLot(99999L, "alice", null, 0))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unLecteurNePeutRienModifier() {
        partagesService.partager(listeId, "alice", "bob");
        Cadeau c = ajouter("X", "5", Statut.A_ACHETER, false, false);
        CadeauCommande commande = new CadeauCommande("Y", null, null, mamanId, false, false, Set.of());

        assertThatThrownBy(() -> cadeauxService.creer(listeId, "bob", commande)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> cadeauxService.changerStatut(listeId, c.getId(), "bob", Statut.REMIS))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> cadeauxService.supprimer(listeId, c.getId(), "bob")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.creer(listeId, "bob", "Z", BigDecimal.ONE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tagsService.creer(listeId, "bob", "t")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> partagesService.partager(listeId, "bob", "carol")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> listesService.renommer(listeId, "bob", "Autre")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> listesService.supprimer(listeId, "bob")).isInstanceOf(AccessDeniedException.class);
        assertThat(cadeaux.findById(c.getId()).orElseThrow().getStatut()).isEqualTo(Statut.A_ACHETER);
    }

    @Test
    void supprimerUnePersonneSupprimeSesCadeaux() {
        ajouter("A", "1", Statut.A_ACHETER, false, false);
        Long autre = personnesService.creer(listeId, "alice", "Papa", BigDecimal.TEN).getId();

        personnesService.supprimer(listeId, mamanId, "alice");

        assertThat(cadeaux.count()).isZero();
        assertThat(personnes.findById(autre)).isPresent();
        assertThat(personnes.findById(mamanId)).isEmpty();
    }

    @Test
    void supprimerUneListeNettoieToutSonContenu() {
        Long tagId = tagsService.creer(listeId, "alice", "promo").getId();
        partagesService.partager(listeId, "alice", "bob");
        cadeauxService.creer(listeId, "alice", new CadeauCommande("A", null, null, mamanId, false, false, Set.of(tagId)));

        listesService.supprimer(listeId, "alice");

        assertThat(listes.count()).isZero();
        assertThat(cadeaux.count()).isZero();
        assertThat(personnes.count()).isZero();
        assertThat(tags.count()).isZero();
        assertThat(partages.count()).isZero();
    }

    @Test
    void personneEtCadeauDUneAutreListeSontIntrouvables() {
        Long autreListe = listesService.creer("alice", "Anniv").getId();
        Cadeau c = ajouter("A", "1", Statut.A_ACHETER, false, false);

        assertThatThrownBy(() -> cadeauxService.changerStatut(autreListe, c.getId(), "alice", Statut.REMIS))
                .isInstanceOf(ElementIntrouvableException.class);
        assertThatThrownBy(() -> personnesService.supprimer(autreListe, mamanId, "alice"))
                .isInstanceOf(ElementIntrouvableException.class);
        assertThatThrownBy(() -> cadeauxService.creer(autreListe, "alice",
                new CadeauCommande("B", null, null, mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void validationDesCadeaux() {
        assertThatThrownBy(() -> cadeauxService.creer(listeId, "alice",
                new CadeauCommande(" ", null, null, mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> cadeauxService.creer(listeId, "alice",
                new CadeauCommande(" ", "https://exemple.fr", null, mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("URL");
        assertThatThrownBy(() -> cadeauxService.creer(listeId, "alice",
                new CadeauCommande("A", "javascript:alert(1)", null, mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> cadeauxService.creer(listeId, "alice",
                new CadeauCommande("A", null, new BigDecimal("-1"), mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> cadeauxService.creer(listeId, "alice",
                new CadeauCommande("A", null, null, null, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void leNomEstDeduitDuTitreDeLUrl() {
        when(titres.titre("https://exemple.fr/produit")).thenReturn(Optional.of("Mon produit"));
        CadeauCommande saisie = new CadeauCommande("", "https://exemple.fr/produit", null, mamanId, false, false, Set.of());

        CadeauCommande complete = cadeauxService.completerNom(saisie);
        Cadeau cadeau = cadeauxService.creer(listeId, "alice", complete);

        assertThat(cadeau.getNom()).isEqualTo("Mon produit");
        assertThat(cadeau.getUrl()).isEqualTo("https://exemple.fr/produit");
    }

    @Test
    void unNomSaisiNEstJamaisRemplaceParLeTitre() {
        CadeauCommande saisie = new CadeauCommande("Mon nom", "https://exemple.fr/produit", null, mamanId, false, false, Set.of());

        assertThat(cadeauxService.completerNom(saisie).nom()).isEqualTo("Mon nom");
    }

    @Test
    void lesTagsSontParListeEtFiltrentLesCadeaux() {
        Long promo = tagsService.creer(listeId, "alice", "promo").getId();
        Long autreListe = listesService.creer("alice", "Anniv").getId();
        Long tagAutreListe = tagsService.creer(autreListe, "alice", "promo").getId();

        cadeauxService.creer(listeId, "alice", new CadeauCommande("Tagué", null, null, mamanId, false, false, Set.of(promo)));
        cadeauxService.creer(listeId, "alice", new CadeauCommande("Libre", null, null, mamanId, false, false, Set.of()));

        assertThatThrownBy(() -> cadeauxService.creer(listeId, "alice",
                new CadeauCommande("X", null, null, mamanId, false, false, Set.of(tagAutreListe))))
                .isInstanceOf(RequeteInvalideException.class);

        VueListe filtre = dashboard.construire(listeId, "alice", promo, 0);
        assertThat(filtre.cadeaux()).extracting(VueCadeau::nom).containsExactly("Tagué");
        assertThat(filtre.cadeaux().getFirst().tags()).containsExactly("promo");
            assertThat(dashboard.chargerLot(listeId, "alice", promo, 0).cadeaux())
                .extracting(VueCadeau::nom).containsExactly("Tagué");
        assertThat(dashboard.construire(listeId, "alice", null, 0).cadeaux()).hasSize(2);
        assertThat(dashboard.construire(listeId, "alice", tagAutreListe, 0).cadeaux()).isEmpty();
    }

    @Test
    void refuseUnTagDejaExistantOuVide() {
        tagsService.creer(listeId, "alice", "promo");

        assertThatThrownBy(() -> tagsService.creer(listeId, "alice", "PROMO")).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> tagsService.creer(listeId, "alice", "  ")).isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void gerePartageEtRetrait() {
        assertThat(partagesService.candidats(listeId, "alice")).containsExactly("bob", "carol");

        partagesService.partager(listeId, "alice", "bob");

        assertThat(partagesService.candidats(listeId, "alice")).containsExactly("carol");
        assertThat(listesService.listesPartagees("bob")).extracting(ListeAchatService.ListeResume::nom).containsExactly("Noel");
        assertThatThrownBy(() -> partagesService.partager(listeId, "alice", "bob")).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> partagesService.partager(listeId, "alice", "alice")).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> partagesService.partager(listeId, "alice", "inconnu")).isInstanceOf(RequeteInvalideException.class);

        Long partageId = partagesService.lister(listeId, "alice").getFirst().id();
        partagesService.retirer(listeId, partageId, "alice");

        assertThat(listesService.listesPartagees("bob")).isEmpty();
        assertThatThrownBy(() -> dashboard.construire(listeId, "bob", null, 0)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void validationDesPersonnesEtListes() {
        assertThatThrownBy(() -> personnesService.creer(listeId, "alice", " ", BigDecimal.ONE)).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> personnesService.creer(listeId, "alice", "Z", new BigDecimal("-5"))).isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> listesService.creer("alice", "")).isInstanceOf(RequeteInvalideException.class);

        Personne sansBudget = personnesService.creer(listeId, "alice", "Sans", null);
        assertThat(sansBudget.getBudget()).isEqualByComparingTo("0");

        personnesService.modifier(listeId, sansBudget.getId(), "alice", "Renommé", new BigDecimal("12"));
        assertThat(personnesService.obtenir(listeId, sansBudget.getId(), "alice").getNom()).isEqualTo("Renommé");

        listesService.renommer(listeId, "alice", "Nouveau nom");
        assertThat(listesService.mesListes("alice")).extracting(ListeAchatService.ListeResume::nom).contains("Nouveau nom");
    }

    @Test
    void pagineLesCadeaux() {
        for (int i = 0; i < DashboardService.TAILLE_PAGE + 3; i++) {
            cadeauxService.creer(listeId, "alice", new CadeauCommande("c" + i, null, null, mamanId, false, false, Set.of()));
        }

        VueListe page0 = dashboard.construire(listeId, "alice", null, 0);
        VueListe page1 = dashboard.construire(listeId, "alice", null, 1);
        DashboardService.LotCadeaux lot0 = dashboard.chargerLot(listeId, "alice", null, 0);
        DashboardService.LotCadeaux lot1 = dashboard.chargerLot(listeId, "alice", null, 1);

        assertThat(page0.cadeaux()).hasSize(DashboardService.TAILLE_PAGE);
        assertThat(page1.cadeaux()).hasSize(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page0.totalCadeaux()).isEqualTo(DashboardService.TAILLE_PAGE + 3);
        assertThat(lot0.cadeaux()).hasSize(DashboardService.TAILLE_PAGE);
        assertThat(lot0.suiteDisponible()).isTrue();
        assertThat(lot1.cadeaux()).hasSize(3);
        assertThat(lot1.suiteDisponible()).isFalse();
            assertThat(lot0.cadeaux().getLast().personneId()).isEqualTo(lot1.cadeaux().getFirst().personneId());
    }
}
