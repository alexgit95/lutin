package com.alexgit95.lutin.transfert;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTransfertWebTest extends BaseTransfertTest {

    @Autowired MockMvc mvc;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return user("alice").roles("USER", "ADMIN");
    }

    private static MockMultipartFile fichier(byte[] contenu) {
        return new MockMultipartFile("fichier", "lutin-export.json", "application/json", contenu);
    }

    @Test
    void lEcranAdminProposeLExportEtLImportAvecLaCleEnModeLocal() throws Exception {
        String html = mvc.perform(get("/admin").with(admin())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Sauvegarde").contains("action=\"/admin/export\"").contains("action=\"/admin/import\"")
                .contains("multipart/form-data").contains("IMPORTER").contains("remplace tout")
                .contains("invitation-key").contains("informations sensibles");
    }

    @Test
    void lExportEstUnTelechargementJsonDateEtNonMisEnCache() throws Exception {
        MvcResult resultat = mvc.perform(post("/admin/export").with(admin()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();

        String contentDisposition = resultat.getResponse().getHeader("Content-Disposition");
        assertThat(resultat.getResponse().getContentType()).startsWith("application/json");
        assertThat(contentDisposition).startsWith("attachment; filename=\"lutin-export-").endsWith(".json\"");
        assertThat(resultat.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .startsWith("{\"format\":\"lutin-export\"").contains("\"login\":\"alice\"");
    }

    @Test
    void exportEtImportSontInterditsAuxNonAdministrateursEtSansCsrf() throws Exception {
        String avant = stable(exporter());

        mvc.perform(post("/admin/export").with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/import").file(fichier(exporter())).param("confirmation", "IMPORTER")
                .with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/export").with(admin())).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/import").file(fichier(exporter())).param("confirmation", "IMPORTER")
                .with(admin())).andExpect(status().isForbidden());

        assertThat(stable(exporter())).isEqualTo(avant);
    }

    @Test
    void unImportValideAvecLaConfirmationRemplaceLaBaseEtAfficheUnCompteRendu() throws Exception {
        byte[] sauvegarde = exporter();
        String attendu = stable(sauvegarde);
        listesService.creer("alice", "Ajoutee apres");

        mvc.perform(multipart("/admin/import").file(fichier(sauvegarde)).param("confirmation", "IMPORTER")
                        .with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("info", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Import terminé"),
                        org.hamcrest.Matchers.containsString("utilisateur 3"))));

        assertThat(stable(exporter())).isEqualTo(attendu);
        mvc.perform(get("/admin").with(admin())).andExpect(status().isOk());
    }

    @Test
    void sansLaConfirmationExacteOuSansFichierRienNEstModifie() throws Exception {
        byte[] sauvegarde = exporter();
        String attendu = stable(sauvegarde);
        listesService.creer("alice", "Ajoutee apres");
        String modifiee = stable(exporter());

        for (String confirmation : new String[]{"", "importer", "IMPORTER "}) {
            mvc.perform(multipart("/admin/import").file(fichier(sauvegarde)).param("confirmation", confirmation)
                            .with(admin()).with(csrf()))
                    .andExpect(redirectedUrl("/admin")).andExpect(flash().attributeExists("erreur"));
        }
        mvc.perform(multipart("/admin/import").param("confirmation", "IMPORTER").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin")).andExpect(flash().attributeExists("erreur"));

        assertThat(stable(exporter())).isEqualTo(modifiee).isNotEqualTo(attendu);
    }

    @Test
    void unFichierInvalideAfficheLErreurSansModifierLaBase() throws Exception {
        String avant = stable(exporter());

        mvc.perform(multipart("/admin/import").file(fichier("pas du json".getBytes(StandardCharsets.UTF_8)))
                        .param("confirmation", "IMPORTER").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("erreur", org.hamcrest.Matchers.containsString("illisible")));

        String sansAdmin = avant.replaceFirst("\"admin_marker\":true", "\"admin_marker\":null");
        mvc.perform(multipart("/admin/import").file(fichier(sansAdmin.getBytes(StandardCharsets.UTF_8)))
                        .param("confirmation", "IMPORTER").with(admin()).with(csrf()))
                .andExpect(flash().attribute("erreur",
                        org.hamcrest.Matchers.containsString("ne contient pas votre compte administrateur")));

        assertThat(stable(exporter())).isEqualTo(avant);
    }
}
