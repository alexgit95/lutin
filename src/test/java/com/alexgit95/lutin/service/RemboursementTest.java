package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AlerteRemboursementRepository;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.vue.VueListe;
import com.alexgit95.lutin.vue.VueRemboursement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RemboursementTest {

    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired PartageService partagesService;
    @Autowired DashboardService dashboard;
    @Autowired RemboursementService remboursements;
    @Autowired AlerteRemboursementService alertesService;
    @Autowired DateDuJour dateDuJour;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;
    @Autowired AlerteRemboursementRepository alertes;

    private Long listeId;
    private Long mamanId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        listeId = listesService.creer("alice", "Noel").getId();
        mamanId = personnesService.creer(listeId, "alice", "Maman", new BigDecimal("500")).getId();
        partagesService.partager(listeId, "alice", "bob");
    }

    @AfterEach
    void nettoyer() {
        alertes.deleteAll();
        cadeaux.deleteAll();
        partages.deleteAll();
        tags.deleteAll();
        personnes.deleteAll();
        listes.deleteAll();
        utilisateurs.deleteAll();
    }

    private Long cadeau(String nom, String prix, Statut statut, boolean tiers) {
        Long id = cadeauxService.creer(listeId, "alice", new CadeauCommande(nom, null,
                prix == null ? null : new BigDecimal(prix), mamanId, false, tiers, Set.of())).getId();
        cadeauxService.changerStatut(listeId, id, "alice", statut);
        return id;
    }

    private LocalDate rembourseLe(Long id) {
        return cadeaux.findById(id).orElseThrow().getRembourseLe();
    }

    private VueRemboursement vue(String login) {
        return remboursements.afficher(listeId, login, 0);
    }

    private List<Long> idsListes(VueRemboursement vue) {
        return vue.cadeaux().stream().map(VueRemboursement.Ligne::id).toList();
    }

    @Test
    void seulsLesCadeauxDebourseAvecUnPrixEtPasPourUnTiersSontRemboursables() {
        Long aAcheter = cadeau("A", "10", Statut.A_ACHETER, false);
        Long commande = cadeau("B", "20", Statut.COMMANDE, false);
        Long recu = cadeau("C", "30", Statut.LIVRE, false);
        Long sansPrix = cadeau("D", null, Statut.COMMANDE, false);
        Long tiers = cadeau("E", "40", Statut.COMMANDE, true);

        VueRemboursement vue = vue("alice");

        assertThat(idsListes(vue)).containsExactlyInAnyOrder(commande, recu)
                .doesNotContain(aAcheter, sansPrix, tiers);
        assertThatThrownBy(() -> cadeauxService.basculerRemboursement(listeId, aAcheter, "alice"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> cadeauxService.basculerRemboursement(listeId, tiers, "alice"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> cadeauxService.basculerRemboursement(listeId, sansPrix, "alice"))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void laBasculeCocheAvecLaDateDuJourPuisDecocheSansAlerte() {
        Long id = cadeau("A", "25", Statut.COMMANDE, false);

        cadeauxService.basculerRemboursement(listeId, id, "alice");
        assertThat(rembourseLe(id)).isEqualTo(dateDuJour.aujourdhui());

        cadeauxService.basculerRemboursement(listeId, id, "alice");
        assertThat(rembourseLe(id)).isNull();
        assertThat(alertes.count()).isZero();
    }

    @Test
    void leMontantARembourserEtRembourseSuitLesCoches() {
        Long a = cadeau("A", "25.50", Statut.COMMANDE, false);
        cadeau("B", "10", Statut.LIVRE, false);
        cadeau("C", "99", Statut.A_ACHETER, false);
        cadeau("D", "50", Statut.COMMANDE, true);

        VueRemboursement avant = vue("alice");
        assertThat(avant.aRembourser()).isEqualByComparingTo("35.50");
        assertThat(avant.rembourse()).isEqualByComparingTo("0");
        assertThat(avant.aujourdhui()).isEqualTo(dateDuJour.aujourdhui());

        cadeauxService.basculerRemboursement(listeId, a, "alice");

        VueRemboursement apres = vue("alice");
        assertThat(apres.aRembourser()).isEqualByComparingTo("10");
        assertThat(apres.rembourse()).isEqualByComparingTo("25.50");
    }

    @Test
    void leRembourserToutMarqueSeulementLesEligiblesNonRembourses() {
        Long deja = cadeau("A", "10", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, deja, "alice");
        LocalDate hier = dateDuJour.aujourdhui().minusDays(1);
        Long b = cadeau("B", "20", Statut.COMMANDE, false);
        Long c = cadeau("C", "5.25", Statut.LIVRE, false);
        Long aAcheter = cadeau("D", "99", Statut.A_ACHETER, false);
        Long tiers = cadeau("E", "40", Statut.COMMANDE, true);

        CadeauService.ResultatRemboursement r = cadeauxService.rembourserTout(listeId, "alice", hier);

        assertThat(r.nombre()).isEqualTo(2);
        assertThat(r.somme()).isEqualByComparingTo("25.25");
        assertThat(rembourseLe(b)).isEqualTo(hier);
        assertThat(rembourseLe(c)).isEqualTo(hier);
        assertThat(rembourseLe(deja)).isEqualTo(dateDuJour.aujourdhui());
        assertThat(rembourseLe(aAcheter)).isNull();
        assertThat(rembourseLe(tiers)).isNull();
        assertThat(vue("alice").aRembourser()).isEqualByComparingTo("0");
    }

    @Test
    void leRembourserToutSansDateUtiliseAujourdhuiEtRienARembourserDonneZero() {
        Long id = cadeau("A", "10", Statut.COMMANDE, false);

        assertThat(cadeauxService.rembourserTout(listeId, "alice", null).nombre()).isEqualTo(1);
        assertThat(rembourseLe(id)).isEqualTo(dateDuJour.aujourdhui());

        CadeauService.ResultatRemboursement vide = cadeauxService.rembourserTout(listeId, "alice", null);
        assertThat(vide.nombre()).isZero();
        assertThat(vide.somme()).isEqualByComparingTo("0");
    }

    @Test
    void uneDateFutureEstRefuseeSansRienModifier() {
        Long id = cadeau("A", "10", Statut.COMMANDE, false);

        assertThatThrownBy(() -> cadeauxService.rembourserTout(listeId, "alice", dateDuJour.aujourdhui().plusDays(1)))
                .isInstanceOf(RequeteInvalideException.class);
        assertThat(rembourseLe(id)).isNull();
    }

    @Test
    void leRetourAAcheterEffaceLaDateEtSignaleLeMontantViré() {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);

        assertThat(rembourseLe(id)).isNull();
        AlerteRemboursementService.Alertes liste = alertesService.lister(listeId, "alice");
        assertThat(liste.total()).isEqualTo(1);
        assertThat(liste.recentes().get(0).cadeauNom()).isEqualTo("Montre");
        assertThat(liste.recentes().get(0).montant()).isEqualByComparingTo("80");
    }

    @Test
    void unChangementDeStatutEntreCommandeEtLivreGardeLeRemboursement() {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        cadeauxService.changerStatut(listeId, id, "alice", Statut.LIVRE);

        assertThat(rembourseLe(id)).isEqualTo(dateDuJour.aujourdhui());
        assertThat(alertes.count()).isZero();
    }

    @Test
    void laSuppressionDuCadeauRembourseSignaleLeVirementEnTrop() {
        Long id = cadeau("Livre", "15", Statut.LIVRE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        cadeauxService.supprimer(listeId, id, "alice");

        assertThat(cadeaux.findById(id)).isEmpty();
        assertThat(alertesService.lister(listeId, "alice").recentes()).hasSize(1);
    }

    @Test
    void laSuppressionDunCadeauNonRembourseNeSignaleRien() {
        Long id = cadeau("Livre", "15", Statut.LIVRE, false);

        cadeauxService.supprimer(listeId, id, "alice");

        assertThat(alertes.count()).isZero();
    }

    @Test
    void lePassagePourUnTiersEffaceLaDateEtSignale() {
        Long id = cadeau("Jeu", "40", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        cadeauxService.modifier(listeId, id, "alice", new CadeauCommande("Jeu", null, new BigDecimal("40"),
                mamanId, false, true, Set.of()));

        assertThat(rembourseLe(id)).isNull();
        assertThat(alertesService.lister(listeId, "alice").total()).isEqualTo(1);
    }

    @Test
    void laSuppressionDeLaPersonneSignaleChaqueCadeauRembourse() {
        Long a = cadeau("A", "10", Statut.COMMANDE, false);
        Long b = cadeau("B", "20", Statut.LIVRE, false);
        cadeau("C", "30", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, a, "alice");
        cadeauxService.basculerRemboursement(listeId, b, "alice");

        personnesService.supprimer(listeId, mamanId, "alice");

        assertThat(alertesService.lister(listeId, "alice").recentes())
                .extracting(v -> v.cadeauNom()).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void lePrixDunCadeauRembourseEstVerrouilleMaisPasLeResteNiLaDate() {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        assertThatThrownBy(() -> cadeauxService.modifier(listeId, id, "alice", new CadeauCommande("Montre", null,
                new BigDecimal("70"), mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> cadeauxService.modifier(listeId, id, "alice", new CadeauCommande("Montre", null,
                null, mamanId, false, false, Set.of())))
                .isInstanceOf(RequeteInvalideException.class);

        cadeauxService.modifier(listeId, id, "alice", new CadeauCommande("Montre bleue", null,
                new BigDecimal("80.00"), mamanId, true, false, Set.of()));

        Cadeau relu = cadeaux.findById(id).orElseThrow();
        assertThat(relu.getNom()).isEqualTo("Montre bleue");
        assertThat(relu.getPrix()).isEqualByComparingTo("80");
        assertThat(relu.getRembourseLe()).isEqualTo(dateDuJour.aujourdhui());
        assertThat(cadeauxService.estRembourse(listeId, id, "alice")).isTrue();
    }

    @Test
    void lePrixDunCadeauNonRembourseResteModifiable() {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);

        cadeauxService.modifier(listeId, id, "alice", new CadeauCommande("Montre", null,
                new BigDecimal("70"), mamanId, false, false, Set.of()));

        assertThat(cadeaux.findById(id).orElseThrow().getPrix()).isEqualByComparingTo("70");
        assertThat(cadeauxService.estRembourse(listeId, id, "alice")).isFalse();
    }

    @Test
    void lesAlertesPersistentJusquaLaFermetureEtLeNombreEstLimite() {
        for (int i = 0; i < AlerteRemboursementService.TAILLE + 3; i++) {
            Long id = cadeau("Cadeau " + i, "10", Statut.COMMANDE, false);
            cadeauxService.basculerRemboursement(listeId, id, "alice");
            cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);
        }

        VueRemboursement vue = vue("alice");
        assertThat(vue.alertes()).hasSize(AlerteRemboursementService.TAILLE);
        assertThat(vue.totalAlertes()).isEqualTo(AlerteRemboursementService.TAILLE + 3);
        assertThat(vue.alertes().get(0).cadeauNom()).isEqualTo("Cadeau " + (AlerteRemboursementService.TAILLE + 2));

        Long premiere = vue.alertes().get(0).id();
        alertesService.fermer(listeId, premiere, "alice");
        alertesService.fermer(listeId, premiere, "alice");

        assertThat(vue("alice").totalAlertes()).isEqualTo(AlerteRemboursementService.TAILLE + 2);
    }

    @Test
    void lOngletListeLesNonRembourseEnPremierPuisLesRemboursesParPersonneEtNom() {
        Long lea = personnesService.creer(listeId, "alice", "Lea", BigDecimal.ZERO).getId();
        Long rembourse = cadeau("A rembourse", "30", Statut.COMMANDE, false);
        Long b = cadeau("B", "20", Statut.COMMANDE, false);
        Long a = cadeau("A", "10", Statut.LIVRE, false);
        Long deLea = cadeauxService.creer(listeId, "alice", new CadeauCommande("Z de Lea", null,
                new BigDecimal("5"), lea, false, false, Set.of())).getId();
        cadeauxService.changerStatut(listeId, deLea, "alice", Statut.COMMANDE);
        cadeau("N", "40", Statut.A_ACHETER, false);
        cadeauxService.basculerRemboursement(listeId, rembourse, "alice");

        VueRemboursement vue = vue("alice");

        assertThat(idsListes(vue)).containsExactly(deLea, a, b, rembourse);
        assertThat(vue.cadeaux().get(3).rembourseLe()).isNotNull();
        assertThat(vue.cadeaux().get(0).rembourseLe()).isNull();
    }

    @Test
    void lOngletEstPagineParLotsDe50() {
        for (int i = 0; i < RemboursementService.TAILLE_PAGE + 5; i++) {
            cadeau(String.format("Cadeau %03d", i), "1", Statut.COMMANDE, false);
        }

        VueRemboursement premiere = remboursements.afficher(listeId, "alice", 0);
        VueRemboursement seconde = remboursements.afficher(listeId, "alice", 1);

        assertThat(premiere.cadeaux()).hasSize(RemboursementService.TAILLE_PAGE);
        assertThat(premiere.totalPages()).isEqualTo(2);
        assertThat(seconde.cadeaux()).hasSize(5);
        assertThat(idsListes(premiere)).doesNotContainAnyElementsOf(idsListes(seconde));
    }

    @Test
    void laLigneEtLaCarteSontDisponiblesPourLeCochage() {
        Long id = cadeau("A", "25", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        VueRemboursement.Ligne ligne = remboursements.ligne(listeId, id, "alice");
        VueRemboursement carte = remboursements.carte(listeId, "alice");

        assertThat(ligne.rembourseLe()).matches("\\d{2}/\\d{2}");
        assertThat(carte.cadeaux()).isEmpty();
        assertThat(carte.rembourse()).isEqualByComparingTo("25");
    }

    @Test
    void leDashboardNePorteAucuneDonneeDeRemboursement() {
        Long id = cadeau("A", "10", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);

        VueListe vue = dashboard.construire(listeId, "alice", null, 0);

        assertThat(vue.cadeaux()).hasSize(1);
        assertThat(VueListe.class.getRecordComponents()).extracting(c -> c.getName())
                .doesNotContain("aRembourser", "rembourse", "alertes", "totalAlertes", "filtreARembourser");
    }

    @Test
    void leBadgeCompteLesAlertesPourLeProprietaireSeulement() {
        Long id = cadeau("A", "10", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);

        assertThat(alertesService.compter(listeId, "alice")).isEqualTo(1);
        assertThat(alertesService.compter(listeId, "bob")).isZero();
        assertThat(alertesService.compter(listeId, "carol")).isZero();
        assertThat(alertesService.compter(listeId + 999, "alice")).isZero();
    }

    @Test
    void unLecteurOuUnEtrangerNePeutPasOuvrirLOnglet() {
        cadeau("A", "10", Statut.COMMANDE, false);

        for (String intrus : new String[]{"bob", "carol"}) {
            assertThatThrownBy(() -> remboursements.afficher(listeId, intrus, 0))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> remboursements.carte(listeId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void lesActionsDeRemboursementSontReserveesAuProprietaire() {
        Long id = cadeau("A", "10", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);
        Long alerteId = alertesService.lister(listeId, "alice").recentes().get(0).id();

        for (String intrus : new String[]{"bob", "carol"}) {
            assertThatThrownBy(() -> cadeauxService.basculerRemboursement(listeId, id, intrus))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> cadeauxService.rembourserTout(listeId, intrus, null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> alertesService.lister(listeId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> alertesService.fermer(listeId, alerteId, intrus))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> cadeauxService.estRembourse(listeId, id, intrus))
                    .isInstanceOf(AccessDeniedException.class);
        }
        assertThat(alertes.count()).isEqualTo(1);
    }

    @Test
    void laSuppressionDeLaListeSupprimeSesAlertes() {
        Long id = cadeau("A", "10", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);

        listesService.supprimer(listeId, "alice");

        assertThat(alertes.count()).isZero();
    }
}
