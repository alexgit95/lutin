package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.service.CadeauCommande;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.ListeAchatService;
import com.alexgit95.lutin.service.PartageService;
import com.alexgit95.lutin.service.PersonneService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonnesBudgetWebTest {

    @Autowired MockMvc mvc;
    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired PartageService partagesService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;

    private Long listeId;
    private Long mamanId;
    private Long papaId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        listeId = listesService.creer("alice", "Noel").getId();
        mamanId = personnesService.creer(listeId, "alice", "Maman", new BigDecimal("120")).getId();
        papaId = personnesService.creer(listeId, "alice", "Papa", BigDecimal.ZERO).getId();
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

    private void cadeau(Long personneId, String prix, Statut statut) {
        Long id = cadeauxService.creer(listeId, "alice", new CadeauCommande("Cadeau", null, new BigDecimal(prix),
                personneId, false, false, Set.of())).getId();
        cadeauxService.changerStatut(listeId, id, "alice", statut);
    }

    private String page() throws Exception {
        return mvc.perform(get("/listes/" + listeId + "/personnes").with(user("alice")))
                .andExpect(status().isOk()).andExpect(view().name("personnes"))
                .andReturn().getResponse().getContentAsString();
    }

    private static int occurrences(String texte, String motif) {
        return texte.split(java.util.regex.Pattern.quote(motif), -1).length - 1;
    }

    @Test
    void lesActionsDeChaquePersonneSontUniquementDansUnSousMenu() throws Exception {
        String html = page();

        assertThat(occurrences(html, "<details class=\"menu-actions\"")).isEqualTo(2);
        assertThat(occurrences(html, "+ Cadeau")).isEqualTo(2);
        assertThat(occurrences(html, ">Modifier<")).isEqualTo(2);
        assertThat(occurrences(html, ">Supprimer<")).isEqualTo(2);
        assertThat(html).contains("aria-label=\"Actions pour Maman\"")
                .doesNotContain("class=\"mini\"").doesNotContain(">supprimer<").doesNotContain(">modifier<");
        assertThat(html.indexOf("+ Cadeau")).isGreaterThan(html.indexOf("<details class=\"menu-actions\""));
        assertThat(html).contains("onsubmit=\"return confirm(");
    }

    @Test
    void laBarreMontreLeDepenseLeResteAPayerEtLeBudgetAvecUneDescriptionAccessible() throws Exception {
        cadeau(mamanId, "85", Statut.COMMANDE);
        cadeau(mamanId, "20", Statut.A_ACHETER);

        String html = page();

        assertThat(html).contains("role=\"img\"")
                .contains("aria-label=\"Dépensé 85,00 €, à payer 20,00 €, budget 120,00 €\"")
                .contains("width:70.8%").contains("width:16.7%")
                .contains("dépensé").contains("à payer").contains("budget")
                .doesNotContain("repere-budget").doesNotContain("dépassé de");
    }

    @Test
    void unBudgetNulAfficheLeTexteSansBarre() throws Exception {
        cadeau(papaId, "30", Statut.COMMANDE);

        String html = page();

        assertThat(occurrences(html, "role=\"img\"")).isEqualTo(1);
        assertThat(html).contains("Papa");
    }

    @Test
    void unDepassementAfficheLeRepereLaMentionEtLeStyleRouge() throws Exception {
        cadeau(mamanId, "100", Statut.COMMANDE);
        cadeau(mamanId, "50", Statut.A_ACHETER);

        String html = page();

        assertThat(html).contains("personne-ligne depasse").contains("repere-budget").contains("left:80.0%")
                .contains("dépassé de");
    }

    @Test
    void uneListeSansCadeauAfficheUneBarreVide() throws Exception {
        String html = page();

        assertThat(html).contains("aria-label=\"Dépensé 0,00 €, à payer 0,00 €, budget 120,00 €\"")
                .contains("width:0.0%");
    }

    @Test
    void leFormulaireEtLeSousMenuRestentAccessiblesAuProprietaireSeulement() throws Exception {
        mvc.perform(get("/listes/" + listeId + "/personnes").with(user("alice")))
                .andExpect(content().string(containsString("Ajouter")))
                .andExpect(content().string(not(containsString("Importer depuis une autre liste"))));
        mvc.perform(get("/listes/" + listeId + "/personnes").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get("/listes/" + listeId + "/personnes").with(user("carol"))).andExpect(status().isForbidden());
    }
}
