package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.service.CadeauCommande;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.ListeAchatService;
import com.alexgit95.lutin.service.PartageService;
import com.alexgit95.lutin.service.PersonneService;
import com.alexgit95.lutin.service.TagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

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
class ListesWebTest {

    @Autowired MockMvc mvc;
    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired TagService tagsService;
    @Autowired PartageService partagesService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;

    private Long listeId;
    private Long mamanId;
    private Long visibleId;

    @BeforeEach
    void preparer() {
        nettoyer();
        for (String login : new String[]{"alice", "bob", "carol"}) {
            utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
        }
        listeId = listesService.creer("alice", "Noel").getId();
        mamanId = personnesService.creer(listeId, "alice", "Maman", new BigDecimal("100")).getId();
        partagesService.partager(listeId, "alice", "bob");
        cadeauxService.creer(listeId, "alice", new CadeauCommande("Cadeau secret", "https://secret.example/x",
                new BigDecimal("12.34"), mamanId, true, false, Set.of()));
        Cadeau visible = cadeauxService.creer(listeId, "alice", new CadeauCommande("Cadeau visible",
                "https://visible.example/y", new BigDecimal("20"), mamanId, false, false, Set.of()));
        visibleId = visible.getId();
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

    @Test
    void lePageAccueilListeLesListesEtLesPartages() throws Exception {
        mvc.perform(get("/").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Noel")));
        mvc.perform(get("/").with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Noel")))
                .andExpect(content().string(containsString("alice")));
    }

    @Test
    void creerUneListeRedirigeVersSonDashboard() throws Exception {
        mvc.perform(post("/listes").with(user("alice")).with(csrf()).param("nom", "Anniv"))
                .andExpect(status().is3xxRedirection());
        assertThat(listesService.mesListes("alice")).hasSize(2);
    }

    @Test
    void leProprietaireVoitLeDashboardComplet() throws Exception {
        mvc.perform(get("/listes/" + listeId).with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Budget global")))
                .andExpect(content().string(containsString("Cadeau secret")))
                .andExpect(content().string(containsString("secret.example")))
                .andExpect(content().string(containsString("/personnes")))
                .andExpect(content().string(not(containsString("Ajouter"))));
    }

    @Test
    void unLecteurNeRecoitAucuneDonneeMasquee() throws Exception {
        mvc.perform(get("/listes/" + listeId).with(user("bob"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lecture seule")))
                .andExpect(content().string(containsString("Cadeau secret")))
                .andExpect(content().string(containsString("Cadeau visible")))
                .andExpect(content().string(not(containsString("secret.example"))))
                .andExpect(content().string(not(containsString("12,34"))))
                .andExpect(content().string(containsString("32,34")))
                .andExpect(content().string(not(containsString("/statut"))))
                .andExpect(content().string(not(containsString("/personnes"))))
                .andExpect(content().string(not(containsString("Ajouter"))));
    }

    @Test
    void unLecteurNePeutPasModifier() throws Exception {
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/statut").with(user("bob")).with(csrf())
                .param("statut", "REMIS")).andExpect(status().isForbidden());
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/supprimer").with(user("bob")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/listes/" + listeId + "/personnes").with(user("bob")).with(csrf()).param("nom", "X"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/listes/" + listeId + "/cadeaux/nouveau").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get("/listes/" + listeId + "/personnes").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get("/listes/" + listeId + "/partages").with(user("bob"))).andExpect(status().isForbidden());

        assertThat(cadeaux.findById(visibleId).orElseThrow().getStatut()).isEqualTo(Statut.A_ACHETER);
    }

    @Test
    void unTiersNeVoitPasLaListe() throws Exception {
        mvc.perform(get("/listes/" + listeId).with(user("carol"))).andExpect(status().isForbidden());
    }

    @Test
    void leChangementDeStatutHtmxRenvoieUnFragmentMisAJour() throws Exception {
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/statut").with(user("alice")).with(csrf())
                        .header("HX-Request", "true").param("statut", "COMMANDE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"contenu\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("Déjà déboursé")));

        assertThat(cadeaux.findById(visibleId).orElseThrow().getStatut()).isEqualTo(Statut.COMMANDE);
    }

    @Test
    void leChangementDeStatutSansHtmxRedirige() throws Exception {
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/statut").with(user("alice")).with(csrf())
                        .param("statut", "LIVRE"))
                .andExpect(redirectedUrl("/listes/" + listeId));

        assertThat(cadeaux.findById(visibleId).orElseThrow().getStatut()).isEqualTo(Statut.LIVRE);
    }

    @Test
    void unStatutInconnuEstRefuse() throws Exception {
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/statut").with(user("alice")).with(csrf())
                .param("statut", "NIMPORTE")).andExpect(status().isBadRequest());
    }

    @Test
    void lesEcrituresExigentLeCsrf() throws Exception {
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/statut").with(user("alice"))
                .param("statut", "REMIS")).andExpect(status().isForbidden());
    }

    @Test
    void creerUnCadeauDepuisLeFormulaire() throws Exception {
        mvc.perform(get("/listes/" + listeId + "/cadeaux/nouveau").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Maman")));
        mvc.perform(post("/listes/" + listeId + "/cadeaux").with(user("alice")).with(csrf())
                        .param("nom", "Écharpe").param("prix", "19.90").param("personneId", mamanId.toString()))
                .andExpect(redirectedUrl("/listes/" + listeId));

        assertThat(cadeaux.count()).isEqualTo(3);
    }

    @Test
    void unFormulaireInvalideReaffichePageAvecLErreur() throws Exception {
        mvc.perform(post("/listes/" + listeId + "/cadeaux").with(user("alice")).with(csrf())
                        .param("nom", "").param("personneId", mamanId.toString()))
                .andExpect(status().isOk()).andExpect(view().name("cadeau-form"))
                .andExpect(content().string(containsString("Le nom est obligatoire")));
        mvc.perform(post("/listes/" + listeId + "/cadeaux").with(user("alice")).with(csrf())
                        .param("nom", "X").param("prix", "abc").param("personneId", mamanId.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Prix invalide")));

        assertThat(cadeaux.count()).isEqualTo(2);
    }

    @Test
    void modifierEtSupprimerUnCadeau() throws Exception {
        mvc.perform(get("/listes/" + listeId + "/cadeaux/" + visibleId + "/modifier").with(user("alice")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Cadeau visible")));
        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId).with(user("alice")).with(csrf())
                        .param("nom", "Renommé").param("personneId", mamanId.toString()))
                .andExpect(redirectedUrl("/listes/" + listeId));
        assertThat(cadeaux.findById(visibleId).orElseThrow().getNom()).isEqualTo("Renommé");

        mvc.perform(post("/listes/" + listeId + "/cadeaux/" + visibleId + "/supprimer").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/listes/" + listeId));
        assertThat(cadeaux.findById(visibleId)).isEmpty();
    }

    @Test
    void gererPersonnesTagsEtPartages() throws Exception {
        mvc.perform(get("/listes/" + listeId + "/personnes").with(user("alice"))).andExpect(status().isOk())
                .andExpect(view().name("personnes")).andExpect(content().string(containsString("Maman")))
                .andExpect(content().string(containsString("Ajouter")));
        mvc.perform(post("/listes/" + listeId + "/personnes").with(user("alice")).with(csrf())
                .param("nom", "Papa").param("budget", "50")).andExpect(redirectedUrl("/listes/" + listeId + "/personnes"));
        assertThat(personnes.count()).isEqualTo(2);

        mvc.perform(get("/listes/" + listeId + "/personnes/" + mamanId + "/modifier").with(user("alice")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Maman")));
        mvc.perform(post("/listes/" + listeId + "/personnes/" + mamanId).with(user("alice")).with(csrf())
                .param("nom", "Maman2").param("budget", "80")).andExpect(redirectedUrl("/listes/" + listeId + "/personnes"));
        assertThat(personnes.findById(mamanId).orElseThrow().getBudget()).isEqualByComparingTo("80");
        mvc.perform(post("/listes/" + listeId + "/personnes/" + mamanId + "/supprimer").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/listes/" + listeId + "/personnes"));
        assertThat(cadeaux.count()).isZero();

        mvc.perform(post("/listes/" + listeId + "/tags").with(user("alice")).with(csrf()).param("nom", "promo"))
                .andExpect(redirectedUrl("/listes/" + listeId));
        assertThat(tags.count()).isEqualTo(1);

        mvc.perform(get("/listes/" + listeId + "/partages").with(user("alice"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("carol")));
        mvc.perform(post("/listes/" + listeId + "/partages").with(user("alice")).with(csrf()).param("lecteur", "carol"))
                .andExpect(redirectedUrl("/listes/" + listeId + "/partages"));
        assertThat(partages.count()).isEqualTo(2);
        Long partageId = partagesService.lister(listeId, "alice").getFirst().id();
        mvc.perform(post("/listes/" + listeId + "/partages/" + partageId + "/retirer").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/listes/" + listeId + "/partages"));
        assertThat(partages.count()).isEqualTo(1);
    }

    @Test
    void renommerEtSupprimerLaListe() throws Exception {
        mvc.perform(get("/listes/" + listeId + "/reglages").with(user("alice"))).andExpect(status().isOk())
                .andExpect(view().name("reglages")).andExpect(content().string(containsString("Noel")));
        mvc.perform(get("/listes/" + listeId).with(user("alice")))
                .andExpect(content().string(not(containsString("Supprimer la liste"))));
        mvc.perform(get("/listes/" + listeId + "/reglages").with(user("bob"))).andExpect(status().isForbidden());

        mvc.perform(post("/listes/" + listeId + "/renommer").with(user("alice")).with(csrf()).param("nom", "Nouveau"))
                .andExpect(redirectedUrl("/listes/" + listeId + "/reglages"));
        assertThat(listes.findById(listeId).orElseThrow().getNom()).isEqualTo("Nouveau");

        mvc.perform(post("/listes/" + listeId + "/supprimer").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertThat(listes.count()).isZero();
    }
}
