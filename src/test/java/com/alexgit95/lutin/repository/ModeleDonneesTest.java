package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Partage;
import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Tag;
import com.alexgit95.lutin.domain.Utilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ModeleDonneesTest {

    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;
    @Autowired TransactionTemplate transaction;

    @BeforeEach
    @AfterEach
    void nettoyer() {
        cadeaux.deleteAll();
        partages.deleteAll();
        tags.deleteAll();
        personnes.deleteAll();
        listes.deleteAll();
        utilisateurs.deleteAll();
    }

    private Utilisateur utilisateur(String login) {
        return utilisateurs.saveAndFlush(new Utilisateur(login, "hash"));
    }

    @Test
    void persisteLeGrapheComplet() {
        Utilisateur alice = utilisateur("alice");
        ListeAchat liste = listes.saveAndFlush(new ListeAchat("Noel", alice));
        Personne maman = personnes.saveAndFlush(new Personne("Maman", new BigDecimal("100.50"), liste));
        Tag tag = tags.saveAndFlush(new Tag("cuisine", liste));

        Cadeau cadeau = new Cadeau();
        cadeau.setNom("Robot");
        cadeau.setPersonne(maman);
        cadeau.setListe(liste);
        cadeau.setTags(Set.of(tag));
        Cadeau enregistre = cadeaux.saveAndFlush(cadeau);

        Cadeau relu = transaction.execute(s -> {
            Cadeau c = cadeaux.findById(enregistre.getId()).orElseThrow();
            c.getTags().size();
            return c;
        });
        assertThat(relu.getStatut()).isEqualTo(Statut.A_ACHETER);
        assertThat(relu.getPrix()).isNull();
        assertThat(relu.isMasque()).isFalse();
        assertThat(relu.isPourTiers()).isFalse();
        assertThat(relu.getTags()).extracting(Tag::getNom).containsExactly("cuisine");
        assertThat(personnes.findByListeIdOrderByNom(liste.getId()).getFirst().getBudget())
                .isEqualByComparingTo("100.50");
        assertThat(cadeaux.countByPersonneId(maman.getId())).isEqualTo(1);
        assertThat(cadeaux.findByListeId(liste.getId(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void refuseUnLoginDuplique() {
        utilisateur("alice");
        assertThatThrownBy(() -> utilisateur("alice")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void autoriseUnSeulAdministrateur() {
        Utilisateur a = new Utilisateur("a", "h");
        a.setAdmin(true);
        utilisateurs.saveAndFlush(a);
        utilisateur("b");
        utilisateur("c");

        Utilisateur autre = new Utilisateur("d", "h");
        autre.setAdmin(true);
        assertThatThrownBy(() -> utilisateurs.saveAndFlush(autre)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void lesTagsDeMemeNomPeuventExisterDansDesListesDifferentes() {
        Utilisateur alice = utilisateur("alice");
        ListeAchat l1 = listes.saveAndFlush(new ListeAchat("L1", alice));
        ListeAchat l2 = listes.saveAndFlush(new ListeAchat("L2", alice));
        tags.saveAndFlush(new Tag("promo", l1));
        tags.saveAndFlush(new Tag("promo", l2));

        assertThat(tags.findByListeIdOrderByNom(l1.getId())).hasSize(1);
        assertThat(tags.findByListeIdOrderByNom(l2.getId())).hasSize(1);
    }

    @Test
    void retrouveLesPartagesParLecteur() {
        Utilisateur alice = utilisateur("alice");
        Utilisateur bob = utilisateur("bob");
        ListeAchat liste = listes.saveAndFlush(new ListeAchat("L", alice));
        partages.saveAndFlush(new Partage(liste, bob));

        assertThat(partages.existsByListeIdAndLecteurId(liste.getId(), bob.getId())).isTrue();
        assertThat(partages.existsByListeIdAndLecteurId(liste.getId(), alice.getId())).isFalse();
        assertThat(partages.findByLecteurLoginOrderByListeNom("bob")).hasSize(1);
    }
}
