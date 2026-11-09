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

    private String page(String login, String requete) throws Exception {
        return mvc.perform(get("/listes/" + listeId + requete).with(user(login)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static int occurrences(String texte, String motif) {
        return texte.split(java.util.regex.Pattern.quote(motif), -1).length - 1;
    }

    @Test
    void leProprietaireVoitLaCarteLesPastillesEtLeFiltre() throws Exception {
        cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeau("Livre", "20", Statut.LIVRE, false);
        cadeau("Jeu", "15", Statut.A_ACHETER, false);
        cadeau("Cadeau tiers", "30", Statut.COMMANDE, true);

        String html = page("alice", "");

        assertThat(html).contains("id=\"carte-remboursement\"").contains("À virer :").contains("100,00")
                .contains("value=\"" + dateDuJour.aujourdhui() + "\"")
                .contains("Tout marquer remboursé")
                .contains("aRembourser=true");
        assertThat(occurrences(html, "class=\"pastille-remb")).isEqualTo(2);
        assertThat(html).contains("à rembourser</button>");
        assertThat(html).doesNotContain("Virement en trop");
    }

    @Test
    void leBoutonDeRemboursementGroupeEstInactifSiRienARembourser() throws Exception {
        cadeau("Jeu", "15", Statut.A_ACHETER, false);

        String html = page("alice", "");

        assertThat(html).contains("id=\"carte-remboursement\"").contains("disabled");
    }

    @Test
    void lePastilleCocheRepondEnHtmxAvecLaCarteEnOob() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);

        String reponse = mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse")
                        .with(user("alice")).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(reponse).contains("id=\"cadeau-" + id + "\"").contains("remboursé le ")
                .contains("id=\"carte-remboursement\"").contains("id=\"alertes-remboursement\"")
                .contains("hx-swap-oob=\"outerHTML\"");
        assertThat(cadeaux.findById(id).orElseThrow().getRembourseLe()).isEqualTo(dateDuJour.aujourdhui());
    }

    @Test
    void sansHtmxLeCochageRedirigeVersLaListe() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);

        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/rembourse").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/listes/" + listeId));
    }

    @Test
    void leRemboursementGroupeAnnonceLeNombreEtLaSomme() throws Exception {
        cadeau("A", "80", Statut.COMMANDE, false);
        cadeau("B", "20.50", Statut.LIVRE, false);

        mvc.perform(post("/listes/" + listeId + "/remboursements").with(user("alice")).with(csrf())
                        .param("date", dateDuJour.aujourdhui().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/listes/" + listeId))
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
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erreur"));
        assertThat(cadeaux.findById(id).orElseThrow().getRembourseLe()).isNull();
    }

    @Test
    void leFiltreARembourserEtLeChargementContinuNeGardentQueLesCadeauxARembourser() throws Exception {
        Long rembourse = cadeau("Deja", "10", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, rembourse, "alice");
        cadeau("Restant", "20", Statut.COMMANDE, false);
        cadeau("Nonachete", "30", Statut.A_ACHETER, false);

        String html = page("alice", "?aRembourser=true");
        assertThat(html).contains("Restant").doesNotContain("Nonachete").doesNotContain("Deja");

        String lot = mvc.perform(get("/listes/" + listeId + "/cadeaux").param("aRembourser", "true").with(user("alice")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(lot).contains("Restant").doesNotContain("Nonachete").doesNotContain("Deja");
    }

    @Test
    void uneAlertePersisteSurLaPageJusquaSaFermeture() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");

        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + id + "/statut").with(user("alice")).with(csrf())
                        .header("HX-Request", "true").param("statut", "A_ACHETER"))
                .andExpect(status().isOk());

        String html = page("alice", "");
        assertThat(html).contains("Virement en trop").contains("80,00").contains("Montre");
        assertThat(page("alice", "")).contains("Virement en trop");

        Long alerteId = alertes.findAll().get(0).getId();
        mvc.perform(post("/listes/" + listeId + "/alertes/" + alerteId + "/fermer").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/listes/" + listeId));

        assertThat(page("alice", "")).doesNotContain("Virement en trop");
    }

    @Test
    void lesActionsSontInterditesAuLecteurEtALEtranger() throws Exception {
        Long id = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, id, "alice");
        cadeauxService.changerStatut(listeId, id, "alice", Statut.A_ACHETER);
        Long alerteId = alertes.findAll().get(0).getId();

        for (String intrus : new String[]{"bob", "carol"}) {
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
    void lePageDunLecteurNeContientAucuneDonneeDeRemboursement() throws Exception {
        Long a = cadeau("Montre", "80", Statut.COMMANDE, false);
        cadeauxService.basculerRemboursement(listeId, a, "alice");
        Long b = cadeau("Livre", "20", Statut.LIVRE, false);
        cadeauxService.basculerRemboursement(listeId, b, "alice");
        cadeauxService.changerStatut(listeId, b, "alice", Statut.A_ACHETER);
        cadeau("Jeu", "15", Statut.COMMANDE, false);

        String html = page("bob", "?aRembourser=true");
        String lot = mvc.perform(get("/listes/" + listeId + "/cadeaux").param("aRembourser", "true").with(user("bob")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        for (String contenu : new String[]{html, lot}) {
            assertThat(contenu).doesNotContain("carte-remboursement").doesNotContain("alertes-remboursement")
                    .doesNotContain("pastille-remb").doesNotContain("À virer").doesNotContain("Virement en trop")
                    .doesNotContain("remboursé").doesNotContain("à rembourser").doesNotContain("/rembourse")
                    .doesNotContain("aRembourser");
        }
        assertThat(html).contains("Montre").contains("Livre").contains("Jeu");
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
