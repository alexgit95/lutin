package com.alexgit95.lutin.transfert;

import com.alexgit95.lutin.security.AccesCloudflareTest;
import com.alexgit95.lutin.security.CloudflareAccessFilter;
import com.alexgit95.lutin.security.JetonsCloudflare;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AccesCloudflareTest
class TransfertCloudflareTest extends BaseTransfertTest {

    @Autowired MockMvc mvc;

    private static RequestPostProcessor jeton(String email) {
        return requete -> {
            requete.addHeader(CloudflareAccessFilter.EN_TETE, JetonsCloudflare.valide(email));
            return requete;
        };
    }

    private static MockMultipartFile fichier(String contenu) {
        return new MockMultipartFile("fichier", "lutin-export.json", "application/json",
                contenu.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void lExportPorteLeModeCloudflareEtLesEmpreintesDEmailsSansMotDePasse() {
        String json = stable(exporter());

        assertThat(json).contains("\"mode\":\"cloudflare\"").contains("\"email_hmac\":\"" + hmac.hacher("alice@example.org") + "\"")
                .contains("\"password_hash\":null").doesNotContain("alice@example.org");
    }

    @Test
    void unAllerRetourRestitueLesComptesParLeurEmpreinte() {
        byte[] sauvegarde = exporter();
        String attendu = stable(sauvegarde);
        creerUtilisateur("dave", false);

        importer(sauvegarde);

        assertThat(stable(exporter())).isEqualTo(attendu);
        assertThat(utilisateurs.findByEmailHmac(hmac.hacher("alice@example.org")).orElseThrow().isAdmin()).isTrue();
    }

    @Test
    void lAdministrateurDoitSeRetrouverParSonEmpreinteDansLeFichier() {
        String json = stable(exporter());
        Matcher empreinte = Pattern.compile("\"email_hmac\":\"" + hmac.hacher("alice@example.org") + "\"").matcher(json);
        assertThat(empreinte.find()).isTrue();
        String autreSecret = json.replaceFirst(Pattern.quote(empreinte.group()), "\"email_hmac\":\"" + "a".repeat(64) + "\"");

        assertThatThrownBy(() -> importer(autreSecret)).isInstanceOf(ImportException.class)
                .hasMessageContaining("ne contient pas votre compte administrateur");
        assertThat(stable(exporter())).isEqualTo(json);
    }

    @Test
    void unFichierExporteEnModeLocalEstRefuse() {
        String json = stable(exporter());

        assertThatThrownBy(() -> importer(json.replaceFirst("\"mode\":\"cloudflare\"", "\"mode\":\"local\"")))
                .isInstanceOf(ImportException.class).hasMessageContaining("mode");
        assertThat(stable(exporter())).isEqualTo(json);
    }

    @Test
    void lEcranAdminNeMontreQueLaSauvegardeEtLAdministrateurResteConnecteApresLImport() throws Exception {
        String html = mvc.perform(get("/admin").with(jeton("alice@example.org"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Sauvegarde").doesNotContain("invitation-key").doesNotContain("alice@example.org");

        String export = mvc.perform(post("/admin/export").with(jeton("alice@example.org")).with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(export).contains("\"mode\":\"cloudflare\"");

        mvc.perform(multipart("/admin/import").file(fichier(export)).param("confirmation", "IMPORTER")
                        .with(jeton("alice@example.org")).with(csrf()))
                .andExpect(redirectedUrl("/admin")).andExpect(flash().attributeExists("info"));
        mvc.perform(get("/admin").with(jeton("alice@example.org"))).andExpect(status().isOk());
    }

    @Test
    void lExportEtLImportExigentUnJetonValideUnAdministrateurEtLeJetonCsrf() throws Exception {
        String avant = stable(exporter());
        String contenu = new String(exporter(), StandardCharsets.UTF_8);

        mvc.perform(post("/admin/export").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/admin/import").file(fichier(contenu)).param("confirmation", "IMPORTER").with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/export").with(jeton("bob@example.org")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/import").file(fichier(contenu)).param("confirmation", "IMPORTER")
                .with(jeton("bob@example.org")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/export").with(jeton("alice@example.org"))).andExpect(status().isForbidden());

        assertThat(stable(exporter())).isEqualTo(avant);
    }
}
