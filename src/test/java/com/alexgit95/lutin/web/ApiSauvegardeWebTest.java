package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.ApiKey;
import com.alexgit95.lutin.repository.ApiKeyRepository;
import com.alexgit95.lutin.service.CleApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ApiSauvegardeWebTest {

    @Autowired MockMvc mvc;
    @Autowired CleApiService service;
    @Autowired ApiKeyRepository cles;

    private String cle;

    @BeforeEach
    void preparer() {
        cles.deleteAll();
        cle = service.generer("nas-backup").cle();
    }

    @AfterEach
    void nettoyer() {
        cles.deleteAll();
    }

    private ApiKey ligne() {
        return cles.findAll().get(0);
    }

    @Test
    void uneCleValideRecupereLaSauvegardeCompleteSansMiseEnCache() throws Exception {
        MvcResult resultat = mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("\"format\":\"lutin-export\"")))
                .andExpect(content().string(containsString("\"api_key\":[")))
                .andReturn();

        assertThat(resultat.getResponse().getContentType()).startsWith("application/json");
        assertThat(resultat.getResponse().getHeader("Content-Disposition"))
                .startsWith("attachment; filename=\"lutin-export-").endsWith(".json\"");
        assertThat(resultat.getResponse().getContentAsString()).doesNotContain(cle);
    }

    @Test
    void leSchemaBearerEstAccepteSansTenirCompteDeLaCasse() throws Exception {
        mvc.perform(get("/api/sauvegarde").header("Authorization", "bearer " + cle)).andExpect(status().isOk());
    }

    @Test
    void sansSessionNiCsrfNiCookie() throws Exception {
        MvcResult resultat = mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle))
                .andExpect(status().isOk()).andReturn();

        assertThat(resultat.getRequest().getSession(false)).isNull();
        assertThat(resultat.getResponse().getCookies()).isEmpty();
    }

    @Test
    void uneCleAbsenteMalFormeeInconnueOuRevoqueeRenvoieUn401Identique() throws Exception {
        String revoquee = service.generer("ancienne").cle();
        cles.findAll().stream().filter(k -> k.getNom().equals("ancienne")).forEach(cles::delete);
        String[] enTetes = {null, "Bearer", "Bearer ", "Basic " + cle, cle, "Bearer lutin_inconnue", "Bearer " + revoquee,
                "Bearer " + cle + "x"};

        for (String entete : enTetes) {
            var requete = get("/api/sauvegarde");
            if (entete != null) {
                requete.header("Authorization", entete);
            }
            MvcResult resultat = mvc.perform(requete).andExpect(status().isUnauthorized()).andReturn();
            assertThat(resultat.getResponse().getContentAsString()).as(String.valueOf(entete)).isEmpty();
            assertThat(resultat.getResponse().getHeader("WWW-Authenticate")).isNull();
        }
    }

    @Test
    void laMethodePostEstRefuseMemeAvecUneCleValide() throws Exception {
        mvc.perform(post("/api/sauvegarde").header("Authorization", "Bearer " + cle))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void uneCleNeDonneAccesQuACetEndpoint() throws Exception {
        mvc.perform(get("/api/autre").header("Authorization", "Bearer " + cle)).andExpect(status().isForbidden());
        mvc.perform(get("/admin").header("Authorization", "Bearer " + cle))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/import").with(csrf()).header("Authorization", "Bearer " + cle))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/export").with(csrf()).header("Authorization", "Bearer " + cle))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/listes").header("Authorization", "Bearer " + cle)).andExpect(status().is3xxRedirection());
    }

    @Test
    void unAdministrateurConnecteSansCleNeRecupereRienParLApi() throws Exception {
        mvc.perform(get("/api/sauvegarde").with(user("alice").roles("USER", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void laDateDeDerniereUtilisationNeChangeQuePourUnAppelAuthentifie() throws Exception {
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer lutin_inconnue"))
                .andExpect(status().isUnauthorized());
        assertThat(ligne().getDerniereUtilisation()).isNull();

        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle)).andExpect(status().isOk());

        assertThat(ligne().getDerniereUtilisation()).isNotNull().isAfterOrEqualTo(ligne().getCreeLe());
    }

    @Test
    void leJournalNommeLaCleSansJamaisLaReveler(CapturedOutput sortie) throws Exception {
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle)).andExpect(status().isOk());
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer lutin_secret-refuse"))
                .andExpect(status().isUnauthorized());

        assertThat(sortie.getAll()).contains("clé d'API « nas-backup »")
                .doesNotContain(cle).doesNotContain(ligne().getEmpreinte()).doesNotContain("lutin_secret-refuse");
    }
}
