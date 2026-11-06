package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.service.ListeAchatService;
import com.alexgit95.lutin.service.PartageService;
import com.alexgit95.lutin.service.PersonneService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportPersonnesWebTest {

    @Autowired MockMvc mvc;
    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired PartageService partagesService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;

    private Long sourceId;
    private Long cibleId;
    private Long mamanId;
    private Long leaId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        sourceId = listesService.creer("alice", "Noel 2025").getId();
        cibleId = listesService.creer("alice", "Noel 2026").getId();
        mamanId = personnesService.creer(sourceId, "alice", "Maman", new BigDecimal("120")).getId();
        personnesService.creer(sourceId, "alice", "Papa", new BigDecimal("80"));
        leaId = personnesService.creer(sourceId, "alice", "Lea", new BigDecimal("50")).getId();
        personnesService.creer(cibleId, "alice", "Papa", new BigDecimal("10"));
        listesService.creer("carol", "Seule liste");
        partagesService.partager(cibleId, "alice", "bob");
    }

    @AfterEach
    void nettoyer() {
        cadeaux.deleteAll();
        partages.deleteAll();
        tags.deleteAll();
        personnes.deleteAll();
        listes.deleteAll();
        utilisateurs.deleteAll();
    }

    private String url(String suite) {
        return "/listes/" + cibleId + "/personnes" + suite;
    }

    @Test
    void lePanneauDImportNApparaitQueSiUneAutreListeExiste() throws Exception {
        mvc.perform(get(url("")).with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Importer depuis une autre liste")))
                .andExpect(content().string(containsString("Noel 2025 (3)")))
                .andExpect(content().string(not(containsString("Noel 2026 (")))); 
        Long seule = listes.findByProprietaireLoginOrderByNom("carol").get(0).getId();
        mvc.perform(get("/listes/" + seule + "/personnes").with(user("carol"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Importer depuis une autre liste"))));
    }

    @Test
    void lApercuCocheLesPersonnesImportablesEtGriseLesPresentes() throws Exception {
        mvc.perform(get(url("/import")).with(user("alice")).param("source", sourceId.toString()))
                .andExpect(status().isOk()).andExpect(view().name("personnes-import"))
                .andExpect(content().string(containsString("Noel 2025")))
                .andExpect(content().string(containsString("déjà présente")))
                .andExpect(content().string(containsString("Importer 2 personnes")))
                .andExpect(content().string(containsString("value=\"" + mamanId + "\" checked=\"checked\"")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")));
    }

    @Test
    void lImportCreeLesPersonnesEtAfficheUnCompteRendu() throws Exception {
        mvc.perform(post(url("/import")).with(user("alice")).with(csrf())
                        .param("source", sourceId.toString())
                        .param("ids", mamanId.toString(), leaId.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")))
                .andExpect(flash().attribute("info", containsString("2 personnes importées")));

        assertThat(personnes.findByListeIdOrderByNom(cibleId)).extracting(Personne::getNom)
                .containsExactlyInAnyOrder("Papa", "Maman", "Lea");
        mvc.perform(get(url("")).with(user("alice"))
                        .flashAttr("info", "2 personnes importées (170,00 € de budget)"))
                .andExpect(content().string(containsString("2 personnes importées")));
    }

    @Test
    void leCompteRendumentionLesPersonnesIgnorees() throws Exception {
        Long papaSource = personnes.findByListeIdOrderByNom(sourceId).stream()
                .filter(p -> p.getNom().equals("Papa")).findFirst().orElseThrow().getId();

        mvc.perform(post(url("/import")).with(user("alice")).with(csrf())
                        .param("source", sourceId.toString()).param("ids", papaSource.toString()))
                .andExpect(flash().attribute("info", containsString("Aucune personne importée")))
                .andExpect(flash().attribute("info", containsString("1 ignorée")));
    }

    @Test
    void lImportSansCsrfEstRefuse() throws Exception {
        mvc.perform(post(url("/import")).with(user("alice")).param("source", sourceId.toString())
                .param("ids", mamanId.toString())).andExpect(status().isForbidden());
        assertThat(personnes.findByListeIdOrderByNom(cibleId)).hasSize(1);
    }

    @Test
    void unLecteurUnEtrangerOuUneSourcePartageeSontRefuses() throws Exception {
        Long bobListe = listesService.creer("bob", "Liste de bob").getId();
        partagesService.partager(bobListe, "bob", "alice");

        mvc.perform(get(url("/import")).with(user("bob")).param("source", sourceId.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post(url("/import")).with(user("bob")).with(csrf()).param("source", sourceId.toString())
                .param("ids", mamanId.toString())).andExpect(status().isForbidden());
        mvc.perform(get(url("/import")).with(user("carol")).param("source", sourceId.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(url("/import")).with(user("alice")).param("source", bobListe.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post(url("/import")).with(user("alice")).with(csrf()).param("source", bobListe.toString()))
                .andExpect(status().isForbidden());
        assertThat(personnes.findByListeIdOrderByNom(cibleId)).hasSize(1);
    }

    @Test
    void laSourceIdentiqueALaCibleRenvoieUneErreurSurLOnglet() throws Exception {
        mvc.perform(get(url("/import")).with(user("alice")).param("source", cibleId.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(url("")))
                .andExpect(flash().attributeExists("erreur"));
    }

    @Test
    void lesAutresRoutesDeLOngletPersonnesRestentInchangees() throws Exception {
        mvc.perform(post(url("")).with(user("alice")).with(csrf()).param("nom", "Zoe").param("budget", "5"))
                .andExpect(status().is3xxRedirection());
        assertThat(personnes.findByListeIdOrderByNom(cibleId)).extracting(Personne::getNom).contains("Zoe");
    }
}
