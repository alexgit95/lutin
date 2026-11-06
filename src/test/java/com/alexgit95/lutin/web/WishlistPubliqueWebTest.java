package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistPubliqueWebTest {

    @Autowired MockMvc mvc;
    @Autowired WishlistService wishlistService;
    @Autowired SouhaitService souhaitService;
    @Autowired WishlistPartageService partageService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired WishlistRepository wishlists;
    @Autowired SouhaitRepository souhaits;
    @Autowired WishlistPartageRepository partages;
    @Autowired AvisRepository avis;

    private Long wishlistId;
    private Long souhaitId;
    private String jeton;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "dave", "root"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        wishlistId = wishlistService.creer("alice", "Anniversaire").getId();
        partageService.partager(wishlistId, "alice", "bob");
        souhaitId = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Casque audio", null, null)).getId();
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

    private String url(String suite) {
        return "/p/" + jeton + suite;
    }

    private String preneur() {
        return souhaits.findAll().get(0).getPrisParNom();
    }

    @Test
    void unVisiteurSansConnexionConsulteLaWishlistSansVoirLeProprietaire() throws Exception {
        mvc.perform(get(url(""))).andExpect(status().isOk())
                .andExpect(view().name("wishlist-publique"))
                .andExpect(content().string(containsString("Anniversaire")))
                .andExpect(content().string(containsString("Casque audio")))
                .andExpect(content().string(containsString("dialogue-nom")))
                .andExpect(content().string(not(containsString("alice"))))
                .andExpect(content().string(not(containsString("Avis"))))
                .andExpect(content().string(not(containsString("/souhaits/nouveau"))));
    }

    @Test
    void lesPagesPubliquesPortentLesEntetesDeProtection() throws Exception {
        mvc.perform(get(url(""))).andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("name=\"robots\"")));
        mvc.perform(get("/login")).andExpect(header().doesNotExist("X-Robots-Tag"));
    }

    @Test
    void unJetonInconnuOuRevoqueRenvoie404() throws Exception {
        mvc.perform(get("/p/inconnu")).andExpect(status().isNotFound());
        wishlistService.rendrePrivee(wishlistId, "alice");
        mvc.perform(get(url(""))).andExpect(status().isNotFound());
    }

    @Test
    void lesWishlistsPriveesRestentProtegees() throws Exception {
        mvc.perform(get("/wishlists")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/wishlists/" + wishlistId)).andExpect(status().is3xxRedirection());
    }

    @Test
    void laPriseSansCsrfEstRefusee() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).param("nom", "Marie"))
                .andExpect(status().isForbidden());
        assertThat(preneur()).isNull();
    }

    @Test
    void unInvitePrendUnArticleEtLeNomEstAfficheAvecLaMention() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(csrf()).param("nom", "Tante Marie"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")));

        assertThat(preneur()).isEqualTo("Tante Marie");
        mvc.perform(get(url(""))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Pris par")))
                .andExpect(content().string(containsString("Tante Marie")))
                .andExpect(content().string(containsString("(invité)")))
                .andExpect(content().string(not(containsString("data-prendre-url"))));
    }

    @Test
    void unNomVideEstRefuseAvecUnMessage() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(csrf()).param("nom", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erreur"));
        assertThat(preneur()).isNull();
    }

    @Test
    void leNomSaisiEstAfficheCommeDuTexteEtJamaisCommeDuHtml() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(csrf())
                .param("nom", "<script>alert(1)</script>")).andExpect(status().is3xxRedirection());

        mvc.perform(get(url(""))).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)"))));
    }

    @Test
    void liberantUnArticleUnInviteVoitUnAvertissementPuisConfirme() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(csrf()).param("nom", "Marie"));

        mvc.perform(post(url("/souhaits/" + souhaitId + "/libere")).with(csrf()))
                .andExpect(status().isOk()).andExpect(view().name("public-confirmation"))
                .andExpect(content().string(containsString("Attention")))
                .andExpect(content().string(containsString("Marie (invité)")));
        assertThat(preneur()).isEqualTo("Marie");

        mvc.perform(post(url("/souhaits/" + souhaitId + "/libere")).with(csrf()).param("confirme", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")));
        assertThat(preneur()).isNull();
    }

    @Test
    void leProprietaireEtLesLecteursPartagesSontRedirigesVersLeurVue() throws Exception {
        mvc.perform(get(url("")).with(user("alice"))).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlists/" + wishlistId));
        mvc.perform(get(url("")).with(user("bob"))).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlists/" + wishlistId));
    }

    @Test
    void unAutreCompteConnectePrendSousSonLoginSansPopup() throws Exception {
        mvc.perform(get(url("")).with(user("dave"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("dialogue-nom"))))
                .andExpect(content().string(not(containsString("data-prendre-url"))));

        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(user("dave")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(preneur()).isEqualTo("dave");
        mvc.perform(get(url("")).with(user("dave"))).andExpect(content().string(containsString("dave")))
                .andExpect(content().string(not(containsString("(invité)"))));
    }

    @Test
    void leProprietaireNePeutPasPrendreParLeLien() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(user("alice")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(preneur()).isNull();
    }

    @Test
    void aucuneRouteDEditionNExisteSurLeLienPublic() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/supprimer")).with(csrf())).andExpect(status().is4xxClientError());
        mvc.perform(post(url("/supprimer")).with(csrf())).andExpect(status().is4xxClientError());
        mvc.perform(post(url("/renommer")).with(csrf()).param("nom", "X")).andExpect(status().is4xxClientError());
        assertThat(souhaits.count()).isEqualTo(1);
        assertThat(wishlists.count()).isEqualTo(1);
    }

    @Test
    void laVueDuProprietaireResteAveugleMemePourUnArticlePrisParUnInvite() throws Exception {
        mvc.perform(post(url("/souhaits/" + souhaitId + "/pris")).with(csrf()).param("nom", "Tante Marie"));

        mvc.perform(get("/wishlists/" + wishlistId).with(user("alice"))).andExpect(status().isOk())
                .andExpect(view().name("wishlist"))
                .andExpect(content().string(containsString("data-partager")))
                .andExpect(content().string(not(containsString("Tante Marie"))))
                .andExpect(content().string(not(containsString("ruban-pris"))))
                .andExpect(content().string(not(containsString("(invité)"))));
    }

    @Test
    void leBoutonPartagerNApparaitQueSiLaWishlistEstPublique() throws Exception {
        mvc.perform(get("/wishlists/" + wishlistId).with(user("alice")))
                .andExpect(content().string(containsString("data-lien=\"http://localhost/p/" + jeton + "\"")));

        wishlistService.rendrePrivee(wishlistId, "alice");

        mvc.perform(get("/wishlists/" + wishlistId).with(user("alice")))
                .andExpect(content().string(not(containsString("data-partager"))));
    }

    @Test
    void lOngletReglagesPermetDActiverRegenererEtDesactiver() throws Exception {
        wishlistService.rendrePrivee(wishlistId, "alice");
        mvc.perform(get("/wishlists/" + wishlistId + "/reglages").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Rendre publique")))
                .andExpect(content().string(not(containsString("/p/"))));

        mvc.perform(post("/wishlists/" + wishlistId + "/public/activer").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/wishlists/" + wishlistId + "/reglages"));
        String premier = wishlistService.jetonPublic(wishlistId, "alice");
        assertThat(premier).isNotNull();
        mvc.perform(get("/wishlists/" + wishlistId + "/reglages").with(user("alice")))
                .andExpect(content().string(containsString("/p/" + premier)))
                .andExpect(content().string(containsString("Régénérer le lien")));

        mvc.perform(post("/wishlists/" + wishlistId + "/public/regenerer").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(wishlistService.jetonPublic(wishlistId, "alice")).isNotNull().isNotEqualTo(premier);

        mvc.perform(post("/wishlists/" + wishlistId + "/public/desactiver").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(wishlistService.jetonPublic(wishlistId, "alice")).isNull();
    }

    @Test
    void seulLeProprietairePeutGererLeLienPublic() throws Exception {
        for (var visiteur : new org.springframework.test.web.servlet.request.RequestPostProcessor[]{
                user("bob"), user("dave"), user("root").roles("ADMIN")}) {
            for (String action : new String[]{"activer", "regenerer", "desactiver"}) {
                mvc.perform(post("/wishlists/" + wishlistId + "/public/" + action).with(visiteur).with(csrf()))
                        .andExpect(status().isForbidden());
            }
            mvc.perform(get("/wishlists/" + wishlistId + "/reglages").with(visiteur)).andExpect(status().isForbidden());
        }
        assertThat(wishlistService.jetonPublic(wishlistId, "alice")).isEqualTo(jeton);
    }
}
