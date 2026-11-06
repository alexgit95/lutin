package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import com.alexgit95.lutin.service.AvisService;
import com.alexgit95.lutin.service.SouhaitCommande;
import com.alexgit95.lutin.service.SouhaitService;
import com.alexgit95.lutin.service.WishlistPartageService;
import com.alexgit95.lutin.service.WishlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistsWebTest {

    @Autowired MockMvc mvc;
    @Autowired WishlistService wishlistService;
    @Autowired SouhaitService souhaitService;
    @Autowired WishlistPartageService partageService;
    @Autowired AvisService avisService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired WishlistRepository wishlists;
    @Autowired SouhaitRepository souhaits;
    @Autowired WishlistPartageRepository partages;
    @Autowired AvisRepository avis;

    private Long wishlistId;
    private Long souhaitId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol", "dave", "root"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        wishlistId = wishlistService.creer("alice", "Anniversaire").getId();
        partageService.partager(wishlistId, "alice", "bob");
        partageService.partager(wishlistId, "alice", "carol");
        souhaitId = souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("Casque audio", "https://shop.example/casque", null)).getId();
    }

    @AfterEach
    void nettoyer() {
        avis.deleteAll();
        souhaits.deleteAll();
        partages.deleteAll();
        wishlists.deleteAll();
        utilisateurs.deleteAll();
    }

    private String url(String suite) {
        return "/wishlists/" + wishlistId + suite;
    }

    @Test
    void laPageDesWishlistsListeLesMiennesEtLesPartagees() throws Exception {
        mvc.perform(get("/wishlists").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Anniversaire")));
        mvc.perform(get("/wishlists").with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Partagées avec moi")))
                .andExpect(content().string(containsString("Anniversaire")));
    }

    @Test
    void creerUneWishlistRedirigeEtUnDoublonEstRefuse() throws Exception {
        mvc.perform(post("/wishlists").with(user("alice")).with(csrf()).param("nom", "Noel"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/wishlists").with(user("alice")).with(csrf()).param("nom", " NOEL "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/wishlists"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .flash().attributeExists("erreur"));
        assertThat(wishlistService.mesWishlists("alice")).hasSize(2);
    }

    @Test
    void lePostSansCsrfEstRefuse() throws Exception {
        mvc.perform(post("/wishlists").with(user("alice")).param("nom", "Noel")).andExpect(status().isForbidden());
    }

    @Test
    void leProprietaireNeRecoitAucuneDonneeSurLEtatPris() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");
        souhaitService.modifier(wishlistId, souhaitId, "alice",
                new SouhaitCommande("Casque audio v2", "https://shop.example/casque", null));

        mvc.perform(get(url("")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(view().name("wishlist"))
                .andExpect(content().string(containsString("Casque audio v2")))
                .andExpect(content().string(not(containsString("bob"))))
                .andExpect(content().string(not(containsString("Pris par"))))
                .andExpect(content().string(not(containsString("pris"))))
                .andExpect(content().string(not(containsString("libere"))))
                .andExpect(content().string(not(containsString("modifié"))))
                .andExpect(content().string(not(containsString("Avis"))));
        mvc.perform(get(url("/souhaits/" + souhaitId + "/modifier")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("bob"))));
    }

    @Test
    void leLecteurVoitLePreneurEtLesBoutons() throws Exception {
        mvc.perform(get(url("")).with(user("bob"))).andExpect(status().isOk())
                .andExpect(view().name("wishlist-lecteur"))
                .andExpect(content().string(containsString("Je le prends")));

        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(user("bob")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")));

        mvc.perform(get(url("")).with(user("carol"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Pris par")))
                .andExpect(content().string(containsString("bob")))
                .andExpect(content().string(not(containsString("Je le prends"))));
    }

    @Test
    void unArticleDejaPrisRenvoieUneErreurLisible() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");

        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(user("carol")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .flash().attribute("erreur", containsString("bob")));
    }

    @Test
    void laPageDuProprietaireNeMontreQueLesArticlesEtLesReglagesSontDansUnOnglet() throws Exception {
        mvc.perform(get(url("")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Casque audio")))
                .andExpect(content().string(containsString("shop.example")))
                .andExpect(content().string(containsString(url("/reglages"))))
                .andExpect(content().string(not(containsString("Supprimer la wishlist"))))
                .andExpect(content().string(not(containsString("Renommer"))));

        mvc.perform(get(url("/reglages")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(view().name("wishlist-reglages"))
                .andExpect(content().string(containsString("Renommer")))
                .andExpect(content().string(containsString("Supprimer la wishlist")))
                .andExpect(content().string(not(containsString("Casque audio"))));
    }

    @Test
    void renommerRevientSurLOngletReglagesEtUnDoublonEstSignale() throws Exception {
        wishlistService.creer("alice", "Noel");

        mvc.perform(post(url("/renommer")).with(user("alice")).with(csrf()).param("nom", "Fetes"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("/reglages")));
        assertThat(wishlistService.nom(wishlistId, "alice")).isEqualTo("Fetes");

        mvc.perform(post(url("/renommer")).with(user("alice")).with(csrf()).param("nom", "noel"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("/reglages")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .flash().attributeExists("erreur"));
        assertThat(wishlistService.nom(wishlistId, "alice")).isEqualTo("Fetes");
    }

    @Test
    void leLecteurVoitUneTuileDistincteQuandLArticleEstPris() throws Exception {
        mvc.perform(get(url("")).with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ruban-pris"))));

        souhaitService.prendre(wishlistId, souhaitId, "bob");

        mvc.perform(get(url("")).with(user("carol"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("tuile-visuel")))
                .andExpect(content().string(containsString("ruban-pris")))
                .andExpect(content().string(containsString("Libérer")));
        mvc.perform(get(url("")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ruban-pris"))))
                .andExpect(content().string(not(containsString("Libérer"))));
    }

    @Test
    void leProprietaireLesNonDestinatairesEtLAdminSontRefuses() throws Exception {
        for (String login : new String[]{"alice", "dave", "root"}) {
            mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(user(login)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(url("")).with(user("dave"))).andExpect(status().isForbidden());
        mvc.perform(get(url("")).with(user("root").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get(url("/partages")).with(user("root").roles("ADMIN"))).andExpect(status().isForbidden());
        assertThat(souhaits.findAll().get(0).getPrisParNom()).isNull();
    }

    @Test
    void unLecteurNePeutPasEditerNiVoirLesPartages() throws Exception {
        mvc.perform(get(url("/souhaits/nouveau")).with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(post(url("/souhaits")).with(user("bob")).with(csrf()).param("nom", "X"))
                .andExpect(status().isForbidden());
        mvc.perform(post(url("/souhaits/" + souhaitId)).with(user("bob")).with(csrf()).param("nom", "X"))
                .andExpect(status().isForbidden());
        mvc.perform(post(url("/souhaits/" + souhaitId + "/supprimer")).with(user("bob")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(url("/renommer")).with(user("bob")).with(csrf()).param("nom", "X"))
                .andExpect(status().isForbidden());
        mvc.perform(post(url("/supprimer")).with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get(url("/partages")).with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get(url("/reglages")).with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get(url("/reglages")).with(user("root").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post(url("/partages")).with(user("bob")).with(csrf()).param("lecteur", "dave"))
                .andExpect(status().isForbidden());
        assertThat(souhaits.findAll()).hasSize(1);
    }

    @Test
    void libererLArticleDUnAutreAfficheUnAvertissementPuisExigeUneConfirmation() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");

        mvc.perform(post(url("/souhaits/" + souhaitId + "/libere")).with(user("carol")).with(csrf()))
                .andExpect(status().isOk()).andExpect(view().name("souhait-confirmation"))
                .andExpect(content().string(containsString("Attention")))
                .andExpect(content().string(containsString("bob")));
        assertThat(souhaits.findAll().get(0).getPrisParNom()).isEqualTo("bob");

        mvc.perform(post(url("/souhaits/" + souhaitId + "/libere")).with(user("carol")).with(csrf())
                        .param("confirme", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")));
        assertThat(souhaits.findAll().get(0).getPrisParNom()).isNull();
    }

    @Test
    void lePreneurLibereSansAvertissement() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");

        mvc.perform(post(url("/souhaits/" + souhaitId + "/libere")).with(user("bob")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(souhaits.findAll().get(0).getPrisParNom()).isNull();
    }

    @Test
    void lesAvisSontAffichesAvecCompteurPuisFermes() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");
        souhaitService.modifier(wishlistId, souhaitId, "alice",
                new SouhaitCommande("Casque v2", "https://shop.example/casque", null));

        mvc.perform(get("/wishlists").with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Avis")))
                .andExpect(content().string(containsString("a été modifié")))
                .andExpect(content().string(containsString("<span class=\"badge\">1</span>")));
        mvc.perform(get(url("")).with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("a été modifié")));
        mvc.perform(get(url("")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("class=\"badge\""))));

        Long avisId = avisService.lister("bob", 0).getContent().get(0).id();
        mvc.perform(post("/avis/" + avisId + "/fermer").with(user("bob")).with(csrf()).param("retour", wishlistId.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")));
        assertThat(avisService.compter("bob")).isZero();
        mvc.perform(get("/wishlists").with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("a été modifié"))));
    }

    @Test
    void unAvisNeSeFermePasParUnAutreUtilisateur() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");
        souhaitService.modifier(wishlistId, souhaitId, "alice",
                new SouhaitCommande("Casque v2", "https://shop.example/casque", null));
        Long avisDeBob = avisService.lister("bob", 0).getContent().get(0).id();

        for (String intrus : new String[]{"carol", "dave"}) {
            mvc.perform(post("/avis/" + avisDeBob + "/fermer").with(user(intrus)).with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        assertThat(avis.count()).isEqualTo(2);
        mvc.perform(post("/avis/" + avisDeBob + "/fermer").with(user("bob")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(avis.count()).isEqualTo(1);
    }

    @Test
    void lesUrlsDesArticlesSontLimiteesAHttpEtHttps() throws Exception {
        mvc.perform(post(url("/souhaits")).with(user("alice")).with(csrf())
                        .param("nom", "Piege").param("url", "javascript:alert(1)"))
                .andExpect(status().isOk()).andExpect(view().name("souhait-form"))
                .andExpect(content().string(containsString("http://")));
        assertThat(souhaits.count()).isEqualTo(1);
    }

    @Test
    void laSuppressionDeLaWishlistPreviendLesLecteurs() throws Exception {
        souhaitService.prendre(wishlistId, souhaitId, "bob");

        mvc.perform(post(url("/supprimer")).with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/wishlists"));

        mvc.perform(get("/wishlists").with(user("carol"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("a été supprimée")));
        mvc.perform(get("/wishlists").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("a été supprimée"))));
    }
}
