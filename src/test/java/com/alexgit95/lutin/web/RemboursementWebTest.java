package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AlerteRemboursementRepository;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.service.CadeauCommande;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.DateDuJour;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RemboursementWebTest {

    @Autowired MockMvc mvc;
    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired PartageService partagesService;
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
        Long id = cadeauxService.creer(listeId, "alice", new CadeauCommande(nom, null, new BigDecimal(prix),
                mamanId, false, tiers, Set.of())).getId();
        cadeauxService.changerStatut(listeId, id, "alice", statut);
        return id;
    }

    private String page(String login, String chemin) throws Exception {
        return mvc.perform(get("/listes/" + listeId + chemin).with(user(login)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String onglet(String login) throws Exception {
        return page(login, "/remboursements");
    }

    private static int occurrences(String texte, String motif) {
        return texte.split(java.util.regex.Pattern.quote(motif), -1).length - 1;
    }

    /** Un cadeau remboursé puis remis à « À acheter » : crée une alerte. */
    private void creerUneAlerte() {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);
    }

    @Test
    void lOngletMontreLaCarteLesPastillesEtSeulementLesCadeauxRemboursables() throws Exception {
        cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeau("Livre", "20", Statut.LIVRE, false);
        cadeau("Jeu", "15", Statut.A_ACHETER, false);
        cadeau("Cadeau tiers", "30", Statut.COMMANDE, true);

        mvc.perform(get("/listes/" + listeId + "/remboursements").with(user("alice")))
                .andExpect(status().isOk()).andExpect(view().name("remboursement"));
        String html = onglet("alice");

        assertThat(html).contains("id=\"carte-remboursement\"").contains("À virer :").contains("100,00")
                .contains("value=\"" + dateDuJour.aujourdhui() + "\"").contains("Tout marquer remboursé")
                .contains("Montre").contains("Livre").doesNotContain("Jeu").doesNotContain("Cadeau tiers");
        assertThat(occurrences(html, "class=\"pastille-remb")).isEqualTo(2);
        assertThat(html).contains("€ à rembourser").doesNotContain("Virement en trop");
    }

    @Test
    void lesCadeauxNonRemboursesPrecedentLesRembourses() throws Exception {
        Long deja = cadeau("Deja", "10", Statut.COMMANDE, false);
        cadeau("Restant", "20", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, deja, "alice");

        String html = onglet("alice");

        assertThat(html.indexOf("Restant")).isLessThan(html.indexOf("Deja"));
        assertThat(html).contains("remboursé le ");
    }

    @Test
    void leBoutonDeRemboursementGroupeEstInactifSiRienARembourser() throws Exception {
        cadeau("Jeu", "15", Statut.A_ACHETER, false);

        String html = onglet("alice");

        assertThat(html).contains("id=\"carte-remboursement\"").contains("disabled")
                .contains("Aucun cadeau à rembourser");
    }

    @Test
    void lePastilleCocheRepondEnHtmxAvecLaLigneEtLaCarteEnOob() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);

        String reponse = mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse")
                        .with(user("alice")).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(reponse).contains("id=\"remb-" + id + "\"").contains("remboursé le ")
                .contains("id=\"carte-remboursement\"").contains("hx-swap-oob=\"outerHTML\"");
        assertThat(cadeaux.findById(id).orElseThrow().getRembourseLe()).isEqualTo(dateDuJour.aujourdhui());
    }

    @Test
    void sansHtmxLeCochageRedirigeVersLOnglet() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);

        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listes/" + listeId + "/remboursements"));
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse").with(user("alice")).with(csrf())
                        .param("page", "2"))
                .andExpect(redirectedUrl("/listes/" + listeId + "/remboursements?page=2"));
    }

    @Test
    void leRemboursementGroupeAnnonceLeNombreEtLaSommeEtRevientSurLOnglet() throws Exception {
        cadeau("A", "80", Statut.COMMANDE, false);
        cadeau("B", "20.50", Statut.LIVRE, false);

        mvc.perform(post("/listes/" + listeId + "/remboursements").with(user("alice")).with(csrf())
                        .param("date", dateDuJour.aujourdhui().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listes/" + listeId + "/remboursements"))
                .andExpect(flash().attribute("info", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("2 cadeaux marqués remboursés"),
                        org.hamcrest.Matchers.containsString("100,50"))));
        assertThat(cadeaux.findAll()).allSatisfy(c -> assertThat(c.getRembourseLe()).isNotNull());
    }

    @Test
    void uneDateFutureAfficheUneErreurSansRienModifier() throws Exception {
        Long id = cadeau("A", "80", Statut.COMMANDE, false);

        mvc.perform(post("/listes/" + listeId + "/remboursements").with(user("alice")).with(csrf())
                        .param("date", dateDuJour.aujourdhui().plusDays(2).toString()))
                .andExpect(redirectedUrl("/listes/" + listeId + "/remboursements"))
                .andExpect(flash().attributeExists("erreur"));
        assertThat(cadeaux.findById(id).orElseThrow().getRembourseLe()).isNull();
    }

    @Test
    void lAlertePersisteDansLOngletJusquaSaFermetureEtJamaisSurLeDashboard() throws Exception {
        creerUneAlerte();

        assertThat(onglet("alice")).contains("Virement en trop").contains("80,00").contains("Montre");
        assertThat(onglet("alice")).contains("Virement en trop");
        assertThat(page("alice", "")).doesNotContain("Virement en trop").doesNotContain("alertes-remboursement");

        Long alerteId = alertes.findAll().get(0).getId();
        mvc.perform(post("/listes/" + listeId + "/alertes/" + alerteId + "/fermer").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listes/" + listeId + "/remboursements"));

        assertThat(onglet("alice")).doesNotContain("Virement en trop");
    }

    @Test
    void leBadgeDeLOngletCompteLesAlertesSurLesPagesDeLaListe() throws Exception {
        String sans = page("alice", "");
        assertThat(sans).contains("id=\"badge-remboursement\"").containsPattern("id=\"badge-remboursement\"[^>]*hidden");

        creerUneAlerte();

        for (String chemin : new String[]{"", "/remboursements", "/personnes", "/partages", "/reglages"}) {
            String html = page("alice", chemin);
            assertThat(html).containsPattern("id=\"badge-remboursement\"[^>]*>1<")
                    .doesNotContainPattern("id=\"badge-remboursement\"[^>]*hidden");
        }
    }

    @Test
    void leChangementDeStatutDuDashboardMetAJourLeBadgeEnOobSansAlerteNiCarte() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        String reponse = mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/statut")
                        .with(user("alice")).with(csrf()).header("HX-Request", "true").param("statut", "A_ACHETER"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(reponse).containsPattern("id=\"badge-remboursement\"[^>]*>1<").contains("hx-swap-oob=\"outerHTML\"")
                .contains("id=\"workflow\"").doesNotContain("carte-remboursement")
                .doesNotContain("alertes-remboursement").doesNotContain("Virement en trop");
    }

    @Test
    void lesActionsEtLOngletSontInterditsAuLecteurEtALEtranger() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        creerUneAlerte();
        Long alerteId = alertes.findAll().get(0).getId();

        for (String intrus : new String[]{"bob", "carol"}) {
            mvc.perform(get("/listes/" + listeId + "/remboursements").with(user(intrus)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse").with(user(intrus)).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/listes/" + listeId + "/remboursements").with(user(intrus)).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/listes/" + listeId + "/alertes/" + alerteId + "/fermer").with(user(intrus)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        assertThat(alertes.count()).isEqualTo(1);
        assertThat(cadeaux.findById(id).orElseThrow().getRembourseLe()).isNull();
    }

    @Test
    void lesActionsExigentLeJetonCsrf() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);

        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse").with(user("alice")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/listes/" + listeId + "/remboursements").with(user("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void leDashboardGardeTousLesCadeauxSansAucuneDonneeDeRemboursement() throws Exception {
        Long montre = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, montre, "alice");
        cadeau("Jeu", "15", Statut.A_ACHETER, false);
        cadeau("Cadeau tiers", "30", Statut.COMMANDE, true);
        creerUneAlerte();

        String html = page("alice", "?aRembourser=true");
        String lot = mvc.perform(get("/listes/" + listeId + "/cadeaux").param("aRembourser", "true").with(user("alice")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        for (String contenu : new String[]{html, lot}) {
            assertThat(contenu).doesNotContain("pastille-remb").doesNotContain("carte-remboursement")
                    .doesNotContain("alertes-remboursement").doesNotContain("À virer")
                    .doesNotContain("Virement en trop").doesNotContain("remboursé le")
                    .doesNotContain("aRembourser").doesNotContain(">À rembourser<");
        }
        assertThat(html).contains("Montre").contains("Jeu").contains("Cadeau tiers")
                .contains("id=\"workflow\"").contains("class=\"statut-form\"");
        assertThat(html).contains(">Remboursement<span");
    }

    @Test
    void unLecteurNeVoitNiLOngletNiLeBadgeNiAucuneDonneeDeRemboursement() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        creerUneAlerte();

        String html = page("bob", "?aRembourser=true");
        String lot = mvc.perform(get("/listes/" + listeId + "/cadeaux").param("aRembourser", "true").with(user("bob")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        for (String contenu : new String[]{html, lot}) {
            assertThat(contenu).doesNotContain("badge-remboursement").doesNotContain("/remboursements")
                    .doesNotContain("pastille-remb").doesNotContain("carte-remboursement")
                    .doesNotContain("Virement en trop").doesNotContain("remboursé").doesNotContain("à rembourser")
                    .doesNotContain("/rembourse").doesNotContain("aRembourser");
        }
        assertThat(html).contains("Montre");
    }

    @Test
    void leFormulaireDunCadeauRembourseAffichePrixEnLectureSeule() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        Long libre = cadeau("Livre", "20", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        String verrouille = mvc.perform(get("/listes/" + listeId + "/cadeaux/" + id + "/modifier").with(user("alice")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String normal = mvc.perform(get("/listes/" + listeId + "/cadeaux/" + libre + "/modifier").with(user("alice")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(verrouille).contains("readonly=\"readonly\"").contains("d'abord le remboursement");
        assertThat(normal).doesNotContain("readonly").doesNotContain("d'abord le remboursement");
    }
}
