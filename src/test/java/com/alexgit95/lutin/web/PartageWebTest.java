package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import com.alexgit95.lutin.service.PartageLien;
import com.alexgit95.lutin.service.SouhaitCommande;
import com.alexgit95.lutin.service.SouhaitService;
import com.alexgit95.lutin.service.UrlTitleService;
import com.alexgit95.lutin.service.WishlistPartageService;
import com.alexgit95.lutin.service.WishlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PartageWebTest {

    private static final String URL = "https://shop.example/objet?a=1&b=%2F+z#fiche";

    @Autowired MockMvc mvc;
    @Autowired WishlistService wishlistService;
    @Autowired SouhaitService souhaitService;
    @Autowired WishlistPartageService partageService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired WishlistRepository wishlists;
    @Autowired WishlistPartageRepository partages;
    @Autowired SouhaitRepository souhaits;
    @Autowired AvisRepository avis;
    @Autowired PasswordEncoder motsDePasse;
    @MockitoBean UrlTitleService titres;

    @BeforeEach
    void preparer() {
        nettoyer();
        utilisateurs.saveAndFlush(new Utilisateur("alice", "hash"));
        utilisateurs.saveAndFlush(new Utilisateur("bob", "hash"));
    }

    @AfterEach
    void nettoyer() {
        avis.deleteAll();
        souhaits.deleteAll();
        partages.deleteAll();
        wishlists.deleteAll();
        utilisateurs.deleteAll();
    }

    @Test
    void proposeUnePremiereWishlistSansCreerAuGet() throws Exception {
        mvc.perform(get("/partage").with(user("alice")).param("url", URL).param("title", "Objet"))
                .andExpect(status().isOk()).andExpect(view().name("partage"))
                .andExpect(model().attribute("partage", new PartageLien(URL, "Objet")))
                .andExpect(content().string(containsString("/partage/wishlists")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
        assertThat(wishlists.count()).isZero();
        assertThat(souhaits.count()).isZero();
    }

    @Test
    void imposeLeChoixMemePourUneWishlistEtExclutLesPartagees() throws Exception {
        Long personnelle = wishlistService.creer("alice", "Anniversaire").getId();
        Long partagee = wishlistService.creer("bob", "Confidentielle").getId();
        partageService.partager(partagee, "bob", "alice");
        mvc.perform(get("/partage").with(user("alice")).param("url", URL).param("title", "Objet"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anniversaire")))
                .andExpect(content().string(not(containsString("Confidentielle"))))
                .andExpect(content().string(containsString("/wishlists/" + personnelle + "/souhaits/nouveau?")));
        wishlistService.creer("alice", "Noel");
        mvc.perform(get("/partage").with(user("alice")).param("text", "Voir " + URL))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Noel")))
                .andExpect(content().string(containsString("Anniversaire")))
                .andExpect(model().attribute("partage", new PartageLien(URL, "")));
        assertThat(souhaits.count()).isZero();
    }

    @Test
    void partageInvalideAfficheUneIssueSansDonneesDeWishlist() throws Exception {
        wishlistService.creer("alice", "Confidentielle");
        for (String url : new String[]{"", "javascript:alert(1)", "x".repeat(2049)}) {
            mvc.perform(get("/partage").with(user("alice")).param("url", url))
                    .andExpect(status().isOk()).andExpect(model().attributeExists("erreur"))
                    .andExpect(model().attributeDoesNotExist("partage", "mesWishlists"))
                    .andExpect(content().string(containsString("href=\"/wishlists\"")))
                    .andExpect(content().string(not(containsString("Confidentielle"))));
        }
        assertThat(souhaits.count()).isZero();
    }

    @Test
    void creationConserveLePartageEtRevientAuChoix() throws Exception {
        mvc.perform(post("/partage/wishlists").with(user("alice")).with(csrf())
                        .param("url", URL).param("title", "Mon & bel objet").param("nom", "Noel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ReceptionPartageController.retour(new PartageLien(URL, "Mon & bel objet"))));
        assertThat(wishlistService.mesWishlists("alice")).extracting(WishlistService.WishlistResume::nom)
                .containsExactly("Noel");
        assertThat(souhaits.count()).isZero();
    }

    @Test
    void erreurDeCreationConserveLeContexteEtUnPartageInvalideNeCreeRien() throws Exception {
        mvc.perform(post("/partage/wishlists").with(user("alice")).with(csrf())
                        .param("url", URL).param("title", "Objet").param("nom", " "))
                .andExpect(status().isOk()).andExpect(model().attributeExists("erreur"))
                .andExpect(model().attribute("partage", new PartageLien(URL, "Objet")))
                .andExpect(content().string(containsString("name=\"url\"")));
        mvc.perform(post("/partage/wishlists").with(user("alice")).with(csrf())
                        .param("url", "javascript:alert(1)").param("nom", "Noel"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("erreur"));
        assertThat(wishlists.count()).isZero();
    }

    @Test
    void authentificationEtCsrfRestentObligatoires() throws Exception {
        mvc.perform(get("/partage").param("url", URL)).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/partage/wishlists").with(user("alice")).param("url", URL).param("nom", "Noel"))
                .andExpect(status().isForbidden());
        assertThat(wishlists.count()).isZero();
    }

        @Test
        void preRemplitSansRequeteReseauAvecUnTitreFourni() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        var resultat = mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).with(user("alice"))
                .param("url", URL).param("title", "<script>alert(1)</script>"))
            .andExpect(status().isOk()).andExpect(view().name("souhait-form"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(containsString("href=\"/wishlists/" + wishlistId + "\"")))
            .andReturn();
        SouhaitForm form = (SouhaitForm) resultat.getModelAndView().getModel().get("form");
        assertThat(form.getUrl()).isEqualTo(URL);
        assertThat(form.getNom()).isEqualTo("<script>alert(1)</script>");
        assertThat(form.getPrix()).isNull();
        assertThat(souhaits.count()).isZero();
        verifyNoInteractions(titres);
        }

        @Test
        void recupereUnTitreSeulementApresSelectionEtLeBorne() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        mvc.perform(get("/partage").with(user("alice")).param("url", URL)).andExpect(status().isOk());
        verifyNoInteractions(titres);
        when(titres.titre(URL)).thenReturn(Optional.of("  " + "n".repeat(150) + "\n "));
        var resultat = mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).with(user("alice"))
                .param("url", URL).param("title", "  "))
            .andExpect(status().isOk()).andReturn();
        SouhaitForm form = (SouhaitForm) resultat.getModelAndView().getModel().get("form");
        assertThat(form.getNom()).isEqualTo("n".repeat(100));
        assertThat(form.getUrl()).isEqualTo(URL);
        verify(titres).titre(URL);
        }

        @Test
        void unTitreIndisponibleLaisseLeNomVideEtLAjoutManuelResteVide() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        when(titres.titre(URL)).thenReturn(Optional.empty());
        var partage = mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).with(user("alice"))
                .param("url", URL))
            .andExpect(status().isOk()).andReturn();
        SouhaitForm form = (SouhaitForm) partage.getModelAndView().getModel().get("form");
        assertThat(form.getNom()).isEmpty();
        assertThat(form.getUrl()).isEqualTo(URL);
        var manuel = mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).with(user("alice")))
            .andExpect(status().isOk()).andReturn();
        SouhaitForm vide = (SouhaitForm) manuel.getModelAndView().getModel().get("form");
        assertThat(vide.getNom()).isNull();
        assertThat(vide.getUrl()).isNull();
        assertThat(vide.getPrix()).isNull();
        verify(titres).titre(URL);
        }

        @Test
        void valeursCorrigeesConserveesApresErreurEtCreationSeulementAuPostValide() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        String chemin = "/wishlists/" + wishlistId + "/souhaits";
        var resultat = mvc.perform(post(chemin).with(user("alice")).with(csrf())
                .param("nom", "Nom corrige").param("url", URL).param("prix", "pas un prix"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("erreur"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andReturn();
        SouhaitForm form = (SouhaitForm) resultat.getModelAndView().getModel().get("form");
        assertThat(form.getNom()).isEqualTo("Nom corrige");
        assertThat(form.getUrl()).isEqualTo(URL);
        assertThat(souhaits.count()).isZero();
        mvc.perform(post(chemin).with(user("alice")).with(csrf())
                .param("nom", "Nom corrige").param("url", URL).param("prix", "12.50"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/wishlists/" + wishlistId));
        assertThat(souhaits.findAll()).singleElement().satisfies(souhait -> {
            assertThat(souhait.getNom()).isEqualTo("Nom corrige");
            assertThat(souhait.getUrl()).isEqualTo(URL);
            assertThat(souhait.getPrix()).isEqualByComparingTo("12.50");
        });
        verifyNoInteractions(titres);
        }

        @Test
        void uneErreurMetierConserveLesCorrectionsEtUnNomToujoursVideEstRefuse() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        String chemin = "/wishlists/" + wishlistId + "/souhaits";
        mvc.perform(post(chemin).with(user("alice")).with(csrf())
                .param("nom", "Nom corrige").param("url", URL).param("prix", "-1"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("erreur"))
            .andExpect(content().string(containsString("Nom corrige")));
        mvc.perform(post(chemin).with(user("alice")).with(csrf()).param("nom", "").param("url", URL))
            .andExpect(status().isOk()).andExpect(model().attributeExists("erreur"));
        assertThat(souhaits.count()).isZero();
        }

        @Test
        void refuseLesDestinationsNonPossedeesAvantToutAccesReseau() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        Long souhaitId = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Secret", "", null)).getId();
        partageService.partager(wishlistId, "alice", "bob");
        String chemin = "/wishlists/" + wishlistId + "/souhaits";
        for (String role : new String[]{"USER", "ADMIN"}) {
            mvc.perform(get(chemin + "/nouveau").with(user("bob").roles(role)).param("url", URL))
                .andExpect(status().isForbidden()).andExpect(model().attributeExists("retourPartage"))
                .andExpect(content().string(not(containsString("Secret"))));
            mvc.perform(post(chemin).with(user("bob").roles(role)).with(csrf())
                    .param("nom", "").param("url", URL))
                .andExpect(status().isForbidden());
            mvc.perform(post(chemin + "/" + souhaitId).with(user("bob").roles(role)).with(csrf())
                    .param("nom", "").param("url", URL))
                .andExpect(status().isForbidden());
        }
        mvc.perform(post(chemin).with(user("alice")).param("nom", "Objet").param("url", URL))
            .andExpect(status().isForbidden());
        assertThat(souhaits.count()).isEqualTo(1);
        verifyNoInteractions(titres);
        }

        @Test
        void uneWishlistSupprimeePermetDeRetrouverLeChoixSansPerdreLePartage() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        wishlistService.supprimer(wishlistId, "alice");
        mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).with(user("alice"))
                .param("url", URL).param("title", "Objet"))
            .andExpect(status().isForbidden()).andExpect(view().name("partage"))
            .andExpect(model().attribute("retourPartage", ReceptionPartageController.retour(new PartageLien(URL, "Objet"))))
            .andExpect(content().string(containsString("Choisir une autre wishlist")));
        verifyNoInteractions(titres);
        assertThat(souhaits.count()).isZero();
        }

        @Test
        void revalideLePartageDansLeFormulaire() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).with(user("alice"))
                .param("url", "javascript:alert(1)"))
            .andExpect(status().isOk()).andExpect(view().name("partage"))
            .andExpect(model().attributeExists("erreur"));
        verifyNoInteractions(titres);
        }

        @Test
        void lesRetoursEncodesConserventLesDonneesSansDestinationExterne() throws Exception {
        String titre = "Mon & bel objet + bleu";
        String retour = ReceptionPartageController.retour(new PartageLien(URL, titre));
        assertThat(retour).startsWith("/partage?url=").contains("%252F%2Bz%23fiche");
        mvc.perform(get(URI.create(retour)).with(user("alice")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("partage", new PartageLien(URL, titre)));
        }

        @Test
        void reprendLePartageApresUneVraieConnexionLocale() throws Exception {
        Utilisateur alice = utilisateurs.findByLogin("alice").orElseThrow();
        alice.setPasswordHash(motsDePasse.encode("motdepasse"));
        utilisateurs.saveAndFlush(alice);
        String cible = ReceptionPartageController.retour(new PartageLien(URL, "Mon & bel objet"));
        var demande = mvc.perform(get(URI.create(cible)).accept(MediaType.TEXT_HTML))
            .andExpect(redirectedUrl("/login")).andReturn();
        MockHttpSession session = (MockHttpSession) demande.getRequest().getSession(false);
        assertThat(session).isNotNull();
        var connexion = mvc.perform(post("/login").session(session).with(csrf())
                .param("username", "alice").param("password", "motdepasse"))
            .andExpect(status().is3xxRedirection()).andReturn();
        String retour = connexion.getResponse().getRedirectedUrl();
        assertThat(retour).startsWith("http://localhost/partage?");
        mvc.perform(get(URI.create(retour)).session(session))
            .andExpect(status().isOk())
            .andExpect(model().attribute("partage", new PartageLien(URL, "Mon & bel objet")));
        }

        @Test
        void deuxPartagesDansLaMemeSessionRestentIndependants() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/partage").session(session).with(user("alice")).param("url", URL).param("title", "Premier"))
            .andExpect(model().attribute("partage", new PartageLien(URL, "Premier")));
        mvc.perform(get("/partage").session(session).with(user("alice"))
                .param("url", "https://autre.example/objet").param("title", "Second"))
            .andExpect(model().attribute("partage", new PartageLien("https://autre.example/objet", "Second")));
        var premier = mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).session(session)
                .with(user("alice")).param("url", URL).param("title", "Premier"))
            .andExpect(status().isOk()).andReturn();
        assertThat(((SouhaitForm) premier.getModelAndView().getModel().get("form")).getUrl()).isEqualTo(URL);
        var second = mvc.perform(get("/wishlists/{id}/souhaits/nouveau", wishlistId).session(session)
                .with(user("alice")).param("url", "https://autre.example/objet").param("title", "Second"))
            .andExpect(status().isOk()).andReturn();
        assertThat(((SouhaitForm) second.getModelAndView().getModel().get("form")).getNom()).isEqualTo("Second");
        verifyNoInteractions(titres);
        }

        @Test
        void lePartageNeReveleAucunEtatPris() throws Exception {
        Long wishlistId = wishlistService.creer("alice", "Noel").getId();
        partageService.partager(wishlistId, "alice", "bob");
        Long souhaitId = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Secret", "", null)).getId();
        souhaitService.prendre(wishlistId, souhaitId, "bob");
        for (String chemin : new String[]{"/partage", "/wishlists/" + wishlistId + "/souhaits/nouveau"}) {
            mvc.perform(get(chemin).with(user("alice")).param("url", URL).param("title", "Objet"))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("bob"))))
                .andExpect(content().string(not(containsString("Pris par"))))
                .andExpect(content().string(not(containsString("Secret"))));
        }
        verifyNoInteractions(titres);
        }
}