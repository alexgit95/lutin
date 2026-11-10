package com.alexgit95.lutin.transfert;

import com.alexgit95.lutin.repository.ApiKeyRepository;
import com.alexgit95.lutin.security.AccesCloudflareTest;
import com.alexgit95.lutin.security.CloudflareAccessFilter;
import com.alexgit95.lutin.security.JetonsCloudflare;
import com.alexgit95.lutin.service.CleApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L'API de sauvegarde ignore Cloudflare Access : un client du réseau local n'a ni jeton ni session. */
@AccesCloudflareTest
class ApiSauvegardeCloudflareTest {

    @Autowired MockMvc mvc;
    @Autowired CleApiService service;
    @Autowired ApiKeyRepository cles;

    private String cle;

    @BeforeEach
    void preparer() {
        cles.deleteAll();
        cle = service.generer("nas").cle();
    }

    @AfterEach
    void nettoyer() {
        cles.deleteAll();
    }

    @Test
    void uneCleValideSuffitSansEnTeteCloudflare() throws Exception {
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer " + cle))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"mode\":\"cloudflare\"")));
    }

    @Test
    void sansCleLaReponseEstUn401() throws Exception {
        mvc.perform(get("/api/sauvegarde")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sauvegarde").header("Authorization", "Bearer lutin_inconnue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unJetonCloudflareSeulNeDonnePasAccesALApi() throws Exception {
        mvc.perform(get("/api/sauvegarde").header(CloudflareAccessFilter.EN_TETE, JetonsCloudflare.valide("a@example.org")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lesAutresRoutesRestentProtegeesParCloudflareMemeAvecUneCle() throws Exception {
        mvc.perform(get("/listes").header("Authorization", "Bearer " + cle)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin").header("Authorization", "Bearer " + cle)).andExpect(status().isUnauthorized());
        assertThat(cles.findAll().get(0).getDerniereUtilisation()).isNull();
    }
}
