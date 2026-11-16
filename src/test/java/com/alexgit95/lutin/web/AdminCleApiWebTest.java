package com.alexgit95.lutin.web;

import com.alexgit95.lutin.config.LutinProperties;
import com.alexgit95.lutin.repository.ApiKeyRepository;
import com.alexgit95.lutin.service.CleApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.ui.ExtendedModelMap;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
class AdminCleApiWebTest {

    @Autowired MockMvc mvc;
    @Autowired CleApiService service;
    @Autowired ApiKeyRepository cles;

    @BeforeEach
    @AfterEach
    void nettoyer() {
        cles.deleteAll();
    }

    private static RequestPostProcessor admin() {
        return user("alice").roles("USER", "ADMIN");
    }

    @Test
    void lEcranAdminAfficheLaSectionDesClesEtAucuneCleSiAucuneNExiste() throws Exception {
        mvc.perform(get("/admin").with(admin())).andExpect(status().isOk())
                                .andExpect(content().string(containsString("id=\"version-build\"")))
                .andExpect(content().string(containsString("Clés d'API de sauvegarde")))
                .andExpect(content().string(containsString("action=\"/admin/cles-api\"")))
                .andExpect(content().string(containsString("Aucune clé")))
                .andExpect(content().string(not(containsString("cle-generee"))));
    }

    @Test
    void laGenerationAfficheLaCleUneSeuleFoisSansRedirectionNiCache() throws Exception {
        MvcResult resultat = mvc.perform(post("/admin/cles-api").with(admin()).with(csrf()).param("nom", "nas-backup"))
                .andExpect(status().isOk()).andExpect(view().name("admin"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("id=\"version-build\"")))
                .andExpect(content().string(containsString("ne sera plus affichée")))
                .andReturn();
        Matcher m = Pattern.compile("id=\"cle-generee\">(lutin_[A-Za-z0-9_-]{43})<").matcher(
                resultat.getResponse().getContentAsString());
        assertThat(m.find()).isTrue();
        String cle = m.group(1);
        assertThat(resultat.getFlashMap().isEmpty()).isTrue();
        assertThat(cles.findAll()).singleElement().satisfies(k -> {
            assertThat(k.getNom()).isEqualTo("nas-backup");
            assertThat(k.getEmpreinte()).isNotEqualTo(cle).doesNotContain(cle);
        });

        mvc.perform(get("/admin").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(cle))))
                .andExpect(content().string(containsString("nas-backup")))
                .andExpect(content().string(containsString("Jamais utilisée")));
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle)).andExpect(status().isOk());
    }

        @Test
        void lesInformationsDeBuildSontFormateesEtFacultatives() {
                verifierVersion(null, "inconnue - inconnu");
                Properties valeurs = new Properties();
                valeurs.setProperty("version", "1.2.3");
                valeurs.setProperty("git.commit", "a1b2c3d4567890");
                verifierVersion(new BuildProperties(valeurs), "1.2.3 - a1b2c3d");
                valeurs.setProperty("git.commit", "unknown");
                verifierVersion(new BuildProperties(valeurs), "1.2.3 - inconnu");
                valeurs.remove("git.commit");
                verifierVersion(new BuildProperties(valeurs), "1.2.3 - inconnu");
                valeurs.setProperty("git.commit", "abcdef");
                verifierVersion(new BuildProperties(valeurs), "1.2.3 - inconnu");
                valeurs.remove("version");
                valeurs.setProperty("git.commit", "0123456789");
                verifierVersion(new BuildProperties(valeurs), "inconnue - 0123456");
        }

        private void verifierVersion(BuildProperties informations, String attendu) {
                ObjectProvider<BuildProperties> fournisseur = mock();
                when(fournisseur.getIfAvailable()).thenReturn(informations);
                AdminController controleur = new AdminController(new LutinProperties("invitation"), service, "local", fournisseur);
                ExtendedModelMap modele = new ExtendedModelMap();
                assertThat(controleur.admin(modele)).isEqualTo("admin");
                assertThat(modele.get("versionBuild")).isEqualTo(attendu);
        }

    @Test
    void lEcranMontreLesDatesDeCreationEtDeDerniereUtilisation() throws Exception {
        String cle = service.generer("nas").cle();
        service.generer("test");
        service.authentifier(cle);

        String html = mvc.perform(get("/admin").with(admin())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).containsPattern("Créée le <span>\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}</span>")
                .containsPattern("Dernière utilisation : <span>\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}</span>")
                .contains("Jamais utilisée");
    }

    @Test
    void unNomInvalideOuDejaPrisRedirigeAvecUnMessageSansCreerDeCle() throws Exception {
        mvc.perform(post("/admin/cles-api").with(admin()).with(csrf()).param("nom", "  "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("erreur", containsString("obligatoire")));
        mvc.perform(post("/admin/cles-api").with(admin()).with(csrf()).param("nom", "x".repeat(101)))
                .andExpect(flash().attribute("erreur", containsString("trop long")));
        service.generer("NAS");
        mvc.perform(post("/admin/cles-api").with(admin()).with(csrf()).param("nom", " nas "))
                .andExpect(flash().attribute("erreur", containsString("déjà")));

        assertThat(cles.count()).isEqualTo(1);
    }

    @Test
    void lePlafondDeClesEstSignale() throws Exception {
        for (int i = 0; i < CleApiService.MAX_CLES; i++) {
            service.generer("cle-" + i);
        }

        mvc.perform(post("/admin/cles-api").with(admin()).with(csrf()).param("nom", "de-trop"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erreur", containsString("20")));
        assertThat(cles.count()).isEqualTo(CleApiService.MAX_CLES);
    }

    @Test
    void laRevocationSupprimeLaCleEtElleNeFonctionnePlus() throws Exception {
        String cle = service.generer("nas").cle();
        service.generer("autre");
        Long id = cles.findAll().stream().filter(k -> k.getNom().equals("nas")).findFirst().orElseThrow().getId();

        mvc.perform(post("/admin/cles-api/" + id + "/revoquer").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("info"));

        assertThat(cles.findAll()).extracting(k -> k.getNom()).containsExactly("autre");
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generationEtRevocationSontReserveesAuxAdministrateursEtProtegeesParCsrf() throws Exception {
        service.generer("nas");
        Long id = cles.findAll().get(0).getId();

        mvc.perform(post("/admin/cles-api").with(user("bob").roles("USER")).with(csrf()).param("nom", "x"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/cles-api/" + id + "/revoquer").with(user("bob").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/cles-api").with(admin()).param("nom", "x")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/cles-api/" + id + "/revoquer").with(admin())).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user("bob").roles("USER"))).andExpect(status().isForbidden());

        assertThat(cles.findAll()).extracting(k -> k.getNom()).containsExactly("nas");
    }
}
