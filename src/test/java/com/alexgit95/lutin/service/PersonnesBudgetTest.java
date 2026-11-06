package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.vue.VueListe;
import com.alexgit95.lutin.vue.VuePersonneBudget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PersonnesBudgetTest {

    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired PartageService partagesService;
    @Autowired DashboardService dashboard;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;

    private Long listeId;
    private Long mamanId;
    private Long papaId;
    private Long leaId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        listeId = listesService.creer("alice", "Noel").getId();
        mamanId = personnesService.creer(listeId, "alice", "Maman", new BigDecimal("120")).getId();
        papaId = personnesService.creer(listeId, "alice", "Papa", BigDecimal.ZERO).getId();
        leaId = personnesService.creer(listeId, "alice", "Lea", new BigDecimal("50")).getId();
        partagesService.partager(listeId, "alice", "bob");
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

    private void cadeau(Long personneId, String prix, Statut statut, boolean masque, boolean tiers) {
        Long id = cadeauxService.creer(listeId, "alice", new CadeauCommande("Cadeau", null,
                prix == null ? null : new BigDecimal(prix), personneId, masque, tiers, Set.of())).getId();
        cadeauxService.changerStatut(listeId, id, "alice", statut);
    }

    private VuePersonneBudget de(Long personneId) {
        return personnesService.lister(listeId, "alice").stream()
                .filter(p -> p.id().equals(personneId)).findFirst().orElseThrow();
    }

    @Test
    void lesPersonnesSontTrieesParNomAvecDepenseEtResteAPayer() {
        cadeau(mamanId, "85", Statut.COMMANDE, false, false);
        cadeau(mamanId, "20", Statut.A_ACHETER, false, false);

        List<VuePersonneBudget> vues = personnesService.lister(listeId, "alice");

        assertThat(vues).extracting(VuePersonneBudget::nom).containsExactly("Lea", "Maman", "Papa");
        VuePersonneBudget maman = de(mamanId);
        assertThat(maman.depense()).isEqualByComparingTo("85");
        assertThat(maman.resteAPayer()).isEqualByComparingTo("20");
        assertThat(maman.prevu()).isEqualByComparingTo("105");
        assertThat(maman.depasse()).isFalse();
        assertThat(maman.pourcentDepense()).isEqualByComparingTo("70.8");
        assertThat(maman.pourcentReste()).isEqualByComparingTo("16.7");
        assertThat(maman.pourcentBudget()).isEqualByComparingTo("100");
    }

    @Test
    void lesReglesDuDashboardSAppliquentParPersonne() {
        cadeau(mamanId, "40", Statut.COMMANDE, false, true);
        cadeau(mamanId, null, Statut.COMMANDE, false, false);
        cadeau(mamanId, null, Statut.A_ACHETER, false, false);
        cadeau(mamanId, "30", Statut.REMIS, true, false);
        cadeau(mamanId, "10", Statut.LIVRE, false, false);
        cadeau(mamanId, "5", Statut.EMBALLE, false, false);
        cadeau(mamanId, "7", Statut.A_ACHETER, true, false);
        cadeau(mamanId, "9", Statut.A_ACHETER, false, true);

        VuePersonneBudget maman = de(mamanId);

        assertThat(maman.depense()).isEqualByComparingTo("45");
        assertThat(maman.resteAPayer()).isEqualByComparingTo("7");
    }

    @Test
    void uneSommeNeSeMelangePasEntreLesPersonnes() {
        cadeau(mamanId, "50", Statut.COMMANDE, false, false);
        cadeau(leaId, "12", Statut.A_ACHETER, false, false);

        assertThat(de(mamanId).resteAPayer()).isEqualByComparingTo("0");
        assertThat(de(leaId).depense()).isEqualByComparingTo("0");
        assertThat(de(leaId).resteAPayer()).isEqualByComparingTo("12");
    }

    @Test
    void unePersonneSansCadeauAUnePartVideEtUnBudgetAZeroSansBarre() {
        VuePersonneBudget lea = de(leaId);
        assertThat(lea.depense()).isEqualByComparingTo("0");
        assertThat(lea.resteAPayer()).isEqualByComparingTo("0");
        assertThat(lea.sansBarre()).isFalse();
        assertThat(lea.pourcentDepense()).isEqualByComparingTo("0");

        cadeau(papaId, "20", Statut.COMMANDE, false, false);
        VuePersonneBudget papa = de(papaId);
        assertThat(papa.sansBarre()).isTrue();
        assertThat(papa.depasse()).isTrue();
        assertThat(papa.depassement()).isEqualByComparingTo("20");
    }

    @Test
    void leDepassementAgrandItLEchelleEtPlaceLeRepereDuBudget() {
        cadeau(mamanId, "100", Statut.COMMANDE, false, false);
        cadeau(mamanId, "50", Statut.A_ACHETER, false, false);

        VuePersonneBudget maman = de(mamanId);

        assertThat(maman.depasse()).isTrue();
        assertThat(maman.depassement()).isEqualByComparingTo("30");
        assertThat(maman.pourcentDepense()).isEqualByComparingTo("66.7");
        assertThat(maman.pourcentReste()).isEqualByComparingTo("33.3");
        assertThat(maman.pourcentBudget()).isEqualByComparingTo("80");
    }

    @Test
    void lesSegmentsNeDepassentJamaisCentPourCentMalgreLesArrondis() {
        Long unTiers = personnesService.creer(listeId, "alice", "Trois", new BigDecimal("3")).getId();
        cadeau(unTiers, "1", Statut.COMMANDE, false, false);
        cadeau(unTiers, "1", Statut.A_ACHETER, false, false);
        cadeau(unTiers, "1", Statut.A_ACHETER, false, false);

        VuePersonneBudget v = de(unTiers);

        assertThat(v.pourcentDepense().add(v.pourcentReste())).isLessThanOrEqualTo(new BigDecimal("100"));
    }

    @Test
    void lesSommesParPersonneEgalentLesTotauxDuDashboard() {
        cadeau(mamanId, "85", Statut.COMMANDE, false, false);
        cadeau(mamanId, "20", Statut.A_ACHETER, false, false);
        cadeau(papaId, "60", Statut.LIVRE, true, false);
        cadeau(papaId, "15", Statut.A_ACHETER, false, false);
        cadeau(leaId, "33", Statut.REMIS, false, true);
        cadeau(leaId, null, Statut.COMMANDE, false, false);
        cadeau(leaId, "8", Statut.EMBALLE, false, false);

        List<VuePersonneBudget> vues = personnesService.lister(listeId, "alice");
        VueListe tableau = dashboard.construire(listeId, "alice", null, 0);

        BigDecimal depense = vues.stream().map(VuePersonneBudget::depense).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = vues.stream().map(VuePersonneBudget::resteAPayer).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(depense).isEqualByComparingTo(tableau.debourse());
        assertThat(reste).isEqualByComparingTo(tableau.resteAPayer());
        assertThat(depense).isEqualByComparingTo("153");
        assertThat(reste).isEqualByComparingTo("35");
    }

    @Test
    void seulLeProprietairePeutListerLesBudgets() {
        assertThatThrownBy(() -> personnesService.lister(listeId, "bob")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.lister(listeId, "carol")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.lister(99999L, "alice")).isInstanceOf(AccessDeniedException.class);
    }
}
