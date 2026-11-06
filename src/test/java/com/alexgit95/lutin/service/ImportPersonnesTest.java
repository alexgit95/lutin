package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ImportPersonnesTest {

    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
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
    private Long papaId;
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
        papaId = personnesService.creer(sourceId, "alice", "Papa", new BigDecimal("80")).getId();
        leaId = personnesService.creer(sourceId, "alice", "Lea", new BigDecimal("50")).getId();
        cadeauxService.creer(sourceId, "alice", new CadeauCommande("Livre", null, new BigDecimal("10"),
                mamanId, false, false, Set.of()));
        personnesService.creer(cibleId, "alice", " papa ", new BigDecimal("10"));
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

    private List<String> nomsDe(Long listeId) {
        return personnes.findByListeIdOrderByNom(listeId).stream().map(Personne::getNom).toList();
    }

    @Test
    void lesSourcesProposeesSontMesAutresListesAvecLeurNombreDePersonnes() {
        Long bobListe = listesService.creer("bob", "Liste de bob").getId();
        personnesService.creer(bobListe, "bob", "Zoe", BigDecimal.TEN);
        partagesService.partager(bobListe, "bob", "alice");

        var sources = personnesService.sourcesPossibles(cibleId, "alice");

        assertThat(sources).extracting(PersonneService.ListeSource::nom).containsExactly("Noel 2025");
        assertThat(sources.get(0).nombrePersonnes()).isEqualTo(3);
        assertThat(personnesService.sourcesPossibles(bobListe, "bob")).isEmpty();
    }

    @Test
    void lApercuSignaleLesPersonnesDejaPresentesSansTenirCompteDeLaCasseNiDesEspaces() {
        var apercu = personnesService.apercuImport(cibleId, sourceId, "alice");

        assertThat(apercu.sourceNom()).isEqualTo("Noel 2025");
        assertThat(apercu.personnes()).extracting(PersonneService.PersonneImportable::nom)
                .containsExactly("Lea", "Maman", "Papa");
        assertThat(apercu.personnes()).filteredOn(PersonneService.PersonneImportable::dejaPresente)
                .extracting(PersonneService.PersonneImportable::nom).containsExactly("Papa");
        assertThat(apercu.nombreImportables()).isEqualTo(2);
        assertThat(apercu.totalImportable()).isEqualByComparingTo("170");
    }

    @Test
    void lImportCopieNomEtBudgetMaisPasLesCadeauxEtIgnoreLesDoublons() {
        long cadeauxAvant = cadeaux.count();

        var resultat = personnesService.importer(cibleId, sourceId, "alice", List.of(mamanId, papaId, leaId));

        assertThat(resultat.importees()).isEqualTo(2);
        assertThat(resultat.ignorees()).isEqualTo(1);
        assertThat(resultat.budgetTotal()).isEqualByComparingTo("170");
        assertThat(nomsDe(cibleId)).containsExactlyInAnyOrder("Lea", "Maman", "papa");
        assertThat(personnes.findByListeIdOrderByNom(cibleId)).filteredOn(p -> p.getNom().equals("Maman"))
                .singleElement().satisfies(p -> assertThat(p.getBudget()).isEqualByComparingTo("120"));
        assertThat(cadeaux.count()).isEqualTo(cadeauxAvant);
        assertThat(nomsDe(sourceId)).containsExactly("Lea", "Maman", "Papa");
    }

    @Test
    void laCopieEstIndependanteDeLaSource() {
        personnesService.importer(cibleId, sourceId, "alice", List.of(mamanId));
        Long copieId = personnes.findByListeIdOrderByNom(cibleId).stream()
                .filter(p -> p.getNom().equals("Maman")).findFirst().orElseThrow().getId();

        personnesService.modifier(cibleId, copieId, "alice", "Maman", new BigDecimal("999"));

        assertThat(personnes.findById(mamanId).orElseThrow().getBudget()).isEqualByComparingTo("120");
    }

    @Test
    void seulesLesPersonnesCocheesSontImportees() {
        var resultat = personnesService.importer(cibleId, sourceId, "alice", List.of(leaId));

        assertThat(resultat.importees()).isEqualTo(1);
        assertThat(nomsDe(cibleId)).containsExactlyInAnyOrder("papa", "Lea");
        assertThat(personnesService.importer(cibleId, sourceId, "alice", null).importees()).isZero();
        assertThat(personnesService.importer(cibleId, sourceId, "alice", List.of()).importees()).isZero();
    }

    @Test
    void unIdentifiantForgeNeCopieRienDUneAutreListe() {
        Long autreListe = listesService.creer("alice", "Autre").getId();
        Long etrangerId = personnesService.creer(autreListe, "alice", "Etranger", BigDecimal.ONE).getId();
        Long bobListe = listesService.creer("bob", "Liste de bob").getId();
        Long secretId = personnesService.creer(bobListe, "bob", "Secret", new BigDecimal("500")).getId();

        var resultat = personnesService.importer(cibleId, sourceId, "alice", List.of(etrangerId, secretId, 99999L));

        assertThat(resultat.importees()).isZero();
        assertThat(nomsDe(cibleId)).containsExactly("papa");
    }

    @Test
    void uneListeNonPossedeeNePeutServirDeSourceNiDeCible() {
        Long bobListe = listesService.creer("bob", "Liste de bob").getId();
        Long secretId = personnesService.creer(bobListe, "bob", "Secret", new BigDecimal("500")).getId();
        partagesService.partager(bobListe, "bob", "alice");
        partagesService.partager(cibleId, "alice", "carol");

        assertThatThrownBy(() -> personnesService.apercuImport(cibleId, bobListe, "alice"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.importer(cibleId, bobListe, "alice", List.of(secretId)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.apercuImport(cibleId, 99999L, "alice"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.apercuImport(cibleId, sourceId, "carol"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.importer(cibleId, sourceId, "carol", List.of(mamanId)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> personnesService.sourcesPossibles(cibleId, "carol"))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(nomsDe(cibleId)).containsExactly("papa");
    }

    @Test
    void laSourceNePeutPasEtreLaCible() {
        assertThatThrownBy(() -> personnesService.apercuImport(cibleId, cibleId, "alice"))
                .isInstanceOf(RequeteInvalideException.class);
        assertThatThrownBy(() -> personnesService.importer(cibleId, cibleId, "alice", List.of()))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void uneSourceVideOuEntierementPresenteNImporteRien() {
        Long vide = listesService.creer("alice", "Vide").getId();
        var apercuVide = personnesService.apercuImport(cibleId, vide, "alice");
        assertThat(apercuVide.personnes()).isEmpty();
        assertThat(apercuVide.nombreImportables()).isZero();
        assertThat(personnesService.importer(cibleId, vide, "alice", List.of(mamanId)).importees()).isZero();

        Long memeNom = listesService.creer("alice", "Meme nom").getId();
        Long pid = personnesService.creer(memeNom, "alice", "PAPA", BigDecimal.TEN).getId();
        var apercu = personnesService.apercuImport(cibleId, memeNom, "alice");
        assertThat(apercu.nombreImportables()).isZero();
        assertThat(personnesService.importer(cibleId, memeNom, "alice", List.of(pid)).ignorees()).isEqualTo(1);
        assertThat(nomsDe(cibleId)).containsExactly("papa");
    }

    @Test
    void unePersonneAjouteeEntreLApercuEtLImportNEstPasDupliquee() {
        personnesService.apercuImport(cibleId, sourceId, "alice");
        personnesService.creer(cibleId, "alice", "maman", BigDecimal.ONE);

        var resultat = personnesService.importer(cibleId, sourceId, "alice", List.of(mamanId, leaId));

        assertThat(resultat.importees()).isEqualTo(1);
        assertThat(resultat.ignorees()).isEqualTo(1);
        assertThat(nomsDe(cibleId)).containsExactlyInAnyOrder("papa", "maman", "Lea");
    }

    @Test
    void deuxHomonymesDeLaSourceSontImportesPuisIgnoresAuSecondImport() {
        Long z1 = personnesService.creer(sourceId, "alice", "Zoe", BigDecimal.TEN).getId();
        Long z2 = personnesService.creer(sourceId, "alice", "Zoe", BigDecimal.ONE).getId();

        assertThat(personnesService.importer(cibleId, sourceId, "alice", List.of(z1, z2)).importees()).isEqualTo(2);
        var second = personnesService.importer(cibleId, sourceId, "alice", List.of(z1, z2, leaId));

        assertThat(second.importees()).isEqualTo(1);
        assertThat(second.ignorees()).isEqualTo(2);
    }
}
