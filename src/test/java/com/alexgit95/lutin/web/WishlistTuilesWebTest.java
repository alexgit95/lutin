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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistTuilesWebTest {

    private static final String URL_ARTICLE = "https://shop.example/casque";

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
    private Long avecUrlId;
    private Long sansUrlId;
    private String jeton;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        wishlistId = wishlistService.creer("alice", "Anniversaire").getId();
        partageService.partager(wishlistId, "alice", "bob");
        avecUrlId = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Casque audio", URL_ARTICLE, null)).getId();
        sansUrlId = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Livre", null, null)).getId();
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

    private String page(String chemin, String login) throws Exception {
        var requete = get(chemin);
        if (login != null) {
            requete = requete.with(user(login));
        }
        return mvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static int occurrences(String texte, String motif) {
        return texte.split(java.util.regex.Pattern.quote(motif), -1).length - 1;
    }

    private void verifierTuiles(String html) {
        assertThat(occurrences(html, "class=\"tuile-lien\"")).isEqualTo(1);
        assertThat(html).contains("href=\"" + URL_ARTICLE + "\"").contains("target=\"_blank\"")
                .contains("rel=\"noopener noreferrer\"");
        assertThat(occurrences(html, "data-copier")).isEqualTo(1);
        assertThat(html).contains("data-lien=\"" + URL_ARTICLE + "\"").contains(">Copier<")
                .doesNotContain(">Voir<").contains("Casque audio").contains("Livre");
    }

    @Test
    void leProprietaireVoitUnLienEtUnBoutonCopierSurLArticleAvecUrlEtRienSurLautre() throws Exception {
        String html = page("/wishlists/" + wishlistId, "alice");

        verifierTuiles(html);
        assertThat(html).contains(">Modifier<").doesNotContain(">Supprimer<").doesNotContain("/supprimer");
    }

    @Test
    void unLecteurVoitLeMemeLienEtLeBoutonCopierAvecJeLePrends() throws Exception {
        String html = page("/wishlists/" + wishlistId, "bob");

        verifierTuiles(html);
        assertThat(html).contains("Je le prends").doesNotContain(">Modifier<");
    }

    @Test
    void laPagePubliqueVoitLeMemeLienEtLeBoutonCopier() throws Exception {
        String html = page("/p/" + jeton, null);

        verifierTuiles(html);
        assertThat(html).contains("Je le prends");
    }

    @Test
    void leBoutonCopierEstEnDehorsDuLienPourNePasOuvrirLArticle() throws Exception {
        String html = page("/wishlists/" + wishlistId, "alice");

        int fermetureDuTitre = html.indexOf("</h3>");
        assertThat(html.indexOf("data-copier")).isGreaterThan(fermetureDuTitre);
        assertThat(html.indexOf(">Modifier<")).isGreaterThan(fermetureDuTitre);
    }

    @Test
    void laSuppressionSeFaitDepuisLEcranDeModificationAvecConfirmation() throws Exception {
        String html = page("/wishlists/" + wishlistId + "/souhaits/" + avecUrlId + "/modifier", "alice");

        assertThat(html).contains("action=\"/wishlists/" + wishlistId + "/souhaits/" + avecUrlId + "/supprimer\"")
                .contains("Supprimer l'article").contains("confirm(");

        mvc.perform(post("/wishlists/" + wishlistId + "/souhaits/" + avecUrlId + "/supprimer")
                        .with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(souhaits.findAll()).extracting(s -> s.getId()).containsExactly(sansUrlId);
    }

    @Test
    void lEcranDAjoutNeProposePasDeSuppression() throws Exception {
        String html = page("/wishlists/" + wishlistId + "/souhaits/nouveau", "alice");

        assertThat(html).doesNotContain("Supprimer").doesNotContain("/supprimer");
    }
}
