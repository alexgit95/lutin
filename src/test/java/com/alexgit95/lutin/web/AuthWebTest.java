package com.alexgit95.lutin.web;

import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthWebTest {

    @Autowired MockMvc mvc;
    @Autowired UtilisateurRepository utilisateurs;

    @BeforeEach
    @AfterEach
    void nettoyer() {
        utilisateurs.deleteAll();
    }

    private org.springframework.test.web.servlet.ResultActions inscrire(String login, String mdp, String cle) throws Exception {
        return mvc.perform(post("/register").with(csrf())
                .param("login", login).param("password", mdp).param("invitationKey", cle));
    }

    @Test
    void inscriptionValideCreeLeCompteEtRedirigeVersLaConnexion() throws Exception {
        inscrire("alice", "motdepasse", "cle-test").andExpect(redirectedUrl("/login?inscrit"));

        assertThat(utilisateurs.findByLogin("alice")).isPresent();
        assertThat(utilisateurs.findByLogin("alice").get().getPasswordHash()).isNotEqualTo("motdepasse");
    }

    @Test
    void cleInvalideEstRefusee() throws Exception {
        inscrire("alice", "motdepasse", "mauvaise").andExpect(status().isOk()).andExpect(view().name("register"));

        assertThat(utilisateurs.count()).isZero();
    }

    @Test
    void loginDejaUtiliseEstRefuse() throws Exception {
        inscrire("alice", "motdepasse", "cle-test");
        inscrire("alice", "autremotdepasse", "cle-test").andExpect(status().isOk()).andExpect(view().name("register"));

        assertThat(utilisateurs.count()).isEqualTo(1);
    }

    @Test
    void motDePasseTropCourtEstRefuse() throws Exception {
        inscrire("alice", "court", "cle-test").andExpect(view().name("register"));

        assertThat(utilisateurs.count()).isZero();
    }

    @Test
    void inscriptionSansCsrfEstRefusee() throws Exception {
        mvc.perform(post("/register").param("login", "alice").param("password", "motdepasse")
                .param("invitationKey", "cle-test")).andExpect(status().isForbidden());
    }

    @Test
    void lePremierInscritEstAdminEtPasLeSecond() throws Exception {
        inscrire("alice", "motdepasse", "cle-test");
        inscrire("bob", "motdepasse", "cle-test");

        assertThat(utilisateurs.findByLogin("alice").get().isAdmin()).isTrue();
        assertThat(utilisateurs.findByLogin("bob").get().isAdmin()).isFalse();
    }

    @Test
    void inscriptionsSimultaneesNeCreentQuUnAdmin() throws Exception {
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            String login = "user" + i;
            threads[i] = Thread.ofPlatform().start(() -> {
                try {
                    inscrire(login, "motdepasse", "cle-test");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        for (Thread t : threads) {
            t.join();
        }

        assertThat(utilisateurs.count()).isEqualTo(4);
        assertThat(utilisateurs.findAll().stream().filter(u -> u.isAdmin()).count()).isEqualTo(1);
    }

    @Test
    void pageProtegeeSansSessionRedirigeVersLaConnexion() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void connexionEstPublique() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ecranAdminAfficheLaCleALAdministrateur() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk()).andExpect(content().string(
                org.hamcrest.Matchers.containsString("cle-test")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void ecranAdminInterditAuxAutres() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }
}
