package com.alexgit95.lutin.security;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AccesCloudflareTest
@ExtendWith(OutputCaptureExtension.class)
class AccesCloudflareWebTest {

    private static final String ALICE = "alice@example.org";
    private static final String BOB = "bob@example.org";

    @Autowired MockMvc mvc;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired EmailHmacService hmac;

    @BeforeEach
    @AfterEach
    void nettoyer() {
        listes.deleteAll();
        utilisateurs.deleteAll();
    }

    private void compte(String pseudo, String email, boolean admin) {
        Utilisateur u = new Utilisateur(pseudo, null);
        u.setEmailHmac(hmac.hacher(email));
        u.setAdmin(admin);
        utilisateurs.saveAndFlush(u);
    }

    private static RequestPostProcessor jeton(String email) {
        return requete -> {
            requete.addHeader(CloudflareAccessFilter.EN_TETE, JetonsCloudflare.valide(email));
            return requete;
        };
    }

    private static RequestPostProcessor jetonBrut(String valeur) {
        return requete -> {
            requete.addHeader(CloudflareAccessFilter.EN_TETE, valeur);
            return requete;
        };
    }

    @Test
    void sansJetonUnePageApplicativeRepondUn401SansRedirection() throws Exception {
        mvc.perform(get("/listes")).andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/")).andExpect(status().isUnauthorized());
        mvc.perform(post("/listes").with(csrf()).param("nom", "X")).andExpect(status().isUnauthorized());
    }

    @Test
    void lesJetonsInvalidesRepondentUn401() throws Exception {
        compte("pseudo-a", ALICE, true);
        Instant demain = Instant.now().plus(Duration.ofDays(1));
        String[] jetons = {
                JetonsCloudflare.jeton(ALICE, JetonsCloudflare.AUDIENCE, JetonsCloudflare.EMETTEUR,
                        Instant.now().minus(Duration.ofHours(1)), JetonsCloudflare.CLE),
                JetonsCloudflare.jeton(ALICE, "autre-application", JetonsCloudflare.EMETTEUR, demain, JetonsCloudflare.CLE),
                JetonsCloudflare.jeton(ALICE, JetonsCloudflare.AUDIENCE, "https://autre.cloudflareaccess.com", demain,
                        JetonsCloudflare.CLE),
                JetonsCloudflare.jeton(ALICE, JetonsCloudflare.AUDIENCE, JetonsCloudflare.EMETTEUR, demain,
                        JetonsCloudflare.AUTRE_CLE),
                JetonsCloudflare.jeton(null, JetonsCloudflare.AUDIENCE, JetonsCloudflare.EMETTEUR, demain,
                        JetonsCloudflare.CLE),
                JetonsCloudflare.jeton("  ", JetonsCloudflare.AUDIENCE, JetonsCloudflare.EMETTEUR, demain,
                        JetonsCloudflare.CLE),
                "n'importe quoi"};

        for (String jeton : jetons) {
            mvc.perform(get("/listes").with(jetonBrut(jeton))).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void lesCheminsPublicsSontServisSansJetonEtSansErreurAvecUnJetonInvalide() throws Exception {
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mvc.perform(get("/manifest.webmanifest")).andExpect(status().isOk());
        mvc.perform(get("/sw.js")).andExpect(status().isOk());
        mvc.perform(get("/css/app.css").with(jetonBrut("n'importe quoi"))).andExpect(status().isOk());
        mvc.perform(get("/p/inexistant")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        mvc.perform(get("/p/inexistant").with(jetonBrut("n'importe quoi"))).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void unCompteExistantAccedeALApplication() throws Exception {
        compte("pseudo-a", ALICE, false);

        mvc.perform(get("/listes").with(jeton(ALICE))).andExpect(status().isOk()).andExpect(view().name("home"));
        mvc.perform(get("/").with(jeton(ALICE))).andExpect(status().isOk()).andExpect(view().name("accueil"));
    }

    @Test
    void lAdresseEstNormaliseeAvantLaRechercheDuCompte() throws Exception {
        compte("pseudo-a", ALICE, false);

        mvc.perform(get("/listes").with(jeton("  Alice@Example.ORG "))).andExpect(status().isOk());
    }

    @Test
    void unCompteSansPseudoEstRenvoyeVersBienvenue() throws Exception {
        mvc.perform(get("/listes").with(jeton(ALICE))).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bienvenue"));
        mvc.perform(get("/bienvenue").with(jeton(ALICE))).andExpect(status().isOk())
                .andExpect(view().name("bienvenue"));
        mvc.perform(get("/listes").with(jeton(ALICE)).header("HX-Request", "true")).andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/bienvenue"));
    }

    @Test
    void unCompteExistantQuitteBienvenue() throws Exception {
        compte("pseudo-a", ALICE, false);

        mvc.perform(get("/bienvenue").with(jeton(ALICE))).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void laPageDeBienvenueEstEpureeAvecUneDeconnexionEtLeManifestAvecIdentifiants() throws Exception {
        String html = mvc.perform(get("/bienvenue").with(jeton(ALICE))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("name=\"pseudo\"").contains("action=\"/logout\"")
                .contains("crossorigin=\"use-credentials\"")
                .doesNotContain("class=\"topbar\"").doesNotContain("Mes listes");
    }

    @Test
    void lAuthentificationNEstPasGardeeEnSessionEtUnAccesRetireCoupeLaSuite() throws Exception {
        compte("pseudo-a", ALICE, false);
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/listes").with(jeton(ALICE)).session(session)).andExpect(status().isOk());
        assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();

        mvc.perform(get("/listes").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void lesChemisDeConnexionLocalSontIntrouvables() throws Exception {
        compte("pseudo-a", ALICE, false);

        mvc.perform(get("/login").with(jeton(ALICE))).andExpect(status().isNotFound());
        mvc.perform(get("/register").with(jeton(ALICE))).andExpect(status().isNotFound());
        mvc.perform(post("/register").with(jeton(ALICE)).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void lEcranAdminResteReserveAuxAdministrateursSansCleDInvitation() throws Exception {
        compte("pseudo-a", ALICE, true);
        compte("pseudo-b", BOB, false);

        String html = mvc.perform(get("/admin").with(jeton(ALICE))).andExpect(status().isOk())
                .andExpect(view().name("admin")).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Sauvegarde")
                .doesNotContain("invitation-key").doesNotContain("INVITATION_KEY").doesNotContain("cle-test");
        mvc.perform(get("/admin").with(jeton(BOB))).andExpect(status().isForbidden());
    }

    @Test
    void laProtectionCsrfResteActive() throws Exception {
        compte("pseudo-a", ALICE, false);

        mvc.perform(post("/listes").with(jeton(ALICE)).param("nom", "X")).andExpect(status().isForbidden());
        mvc.perform(post("/listes").with(jeton(ALICE)).with(csrf()).param("nom", "X"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/bienvenue").with(jeton(BOB)).param("pseudo", "bobby")).andExpect(status().isForbidden());
    }

    @Test
    void laDeconnexionRedirigeVersCloudflare() throws Exception {
        compte("pseudo-a", ALICE, false);

        mvc.perform(post("/logout").with(jeton(ALICE)).with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://" + JetonsCloudflare.EQUIPE + "/cdn-cgi/access/logout"));
    }

    @Test
    void lePremierPseudoCreeLeCompteAdministrateurPuisLesSuivantsNeLeSontPas() throws Exception {
        mvc.perform(post("/bienvenue").with(jeton(ALICE)).with(csrf()).param("pseudo", "Alice1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(post("/bienvenue").with(jeton(BOB)).with(csrf()).param("pseudo", "Bobby"))
                .andExpect(status().is3xxRedirection());

        assertThat(utilisateurs.findByEmailHmac(hmac.hacher(ALICE)).orElseThrow().isAdmin()).isTrue();
        assertThat(utilisateurs.findByEmailHmac(hmac.hacher(BOB)).orElseThrow().isAdmin()).isFalse();
        mvc.perform(get("/admin").with(jeton(ALICE))).andExpect(status().isOk());
        mvc.perform(get("/admin").with(jeton(BOB))).andExpect(status().isForbidden());
        mvc.perform(get("/").with(jeton(ALICE))).andExpect(status().isOk());
    }

    @Test
    void unPseudoInvalideReaffichelFormulaireSansCreerDeCompte() throws Exception {
        mvc.perform(post("/bienvenue").with(jeton(ALICE)).with(csrf()).param("pseudo", "abc"))
                .andExpect(status().isOk()).andExpect(view().name("bienvenue"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entre 4 et 50 caractères")));
        mvc.perform(post("/bienvenue").with(jeton(ALICE)).with(csrf()).param("pseudo", "Admin"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("réservé")));
        mvc.perform(post("/bienvenue").with(jeton(ALICE)).with(csrf()))
                .andExpect(status().isOk()).andExpect(view().name("bienvenue"));

        assertThat(utilisateurs.count()).isZero();
    }

    @Test
    void unPseudoDejaPrisEstRefuseQuelleQueSoitLaCasse() throws Exception {
        compte("Bobby", BOB, true);

        mvc.perform(post("/bienvenue").with(jeton(ALICE)).with(csrf()).param("pseudo", "bOBBY"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("déjà pris")));

        assertThat(utilisateurs.count()).isEqualTo(1);
    }

    @Test
    void lEmailNApparaitNiDansLesPagesNiDansLesJournaux(CapturedOutput sortie) throws Exception {
        String[] fragments = {"alice@example.org", "example.org"};
        String jetonAlice = JetonsCloudflare.valide(ALICE);
        StringBuilder pages = new StringBuilder();

        pages.append(mvc.perform(get("/bienvenue").with(jetonBrut(jetonAlice))).andReturn().getResponse().getContentAsString());
        pages.append(mvc.perform(post("/bienvenue").with(jetonBrut(jetonAlice)).with(csrf()).param("pseudo", "abc"))
                .andReturn().getResponse().getContentAsString());
        pages.append(mvc.perform(post("/bienvenue").with(jetonBrut(jetonAlice)).with(csrf()).param("pseudo", "pseudo-a"))
                .andReturn().getResponse().getContentAsString());
        for (String chemin : new String[]{"/", "/listes", "/wishlists", "/admin"}) {
            pages.append(mvc.perform(get(chemin).with(jetonBrut(jetonAlice))).andReturn().getResponse().getContentAsString());
        }
        mvc.perform(get("/listes").with(jetonBrut(JetonsCloudflare.jeton(ALICE, "autre", JetonsCloudflare.EMETTEUR,
                Instant.now().plus(Duration.ofDays(1)), JetonsCloudflare.CLE))));

        for (String fragment : fragments) {
            assertThat(pages.toString()).doesNotContain(fragment);
            assertThat(sortie.getAll()).doesNotContain(fragment);
        }
        assertThat(sortie.getAll()).doesNotContain(jetonAlice).doesNotContain(hmac.hacher(ALICE));
        assertThat(pages.toString()).doesNotContain(hmac.hacher(ALICE)).contains("pseudo-a");
    }
}
