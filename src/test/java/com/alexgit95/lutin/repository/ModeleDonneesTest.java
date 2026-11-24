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
import org.junit.jupiter.api.io.TempDir;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
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
    void miseAJourSqliteConserveLesPersonnesExistantes(@TempDir Path dossier) throws Exception {
        String url = "jdbc:sqlite:" + dossier.resolve("ancien.db");
        try (var connexion = DriverManager.getConnection(url); var commande = connexion.createStatement()) {
            commande.execute("create table personne (id integer primary key, nom varchar(100) not null, budget numeric(12,2) not null, liste_id bigint not null)");
            commande.execute("insert into personne (id, nom, budget, liste_id) values (1, 'Maman', 25, 1)");
        }
        var registre = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", url)
                .applySetting("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .applySetting("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
                .applySetting("hibernate.hbm2ddl.auto", "update")
                .build();
        try {
            try (var sessionFactory = new MetadataSources(registre).addAnnotatedClass(Personne.class)
                    .addAnnotatedClass(ListeAchat.class).addAnnotatedClass(Utilisateur.class)
                    .buildMetadata().buildSessionFactory()) {
                try (var connexion = DriverManager.getConnection(url); var commande = connexion.createStatement();
                     var resultat = commande.executeQuery("select nom, budget, termine from personne where id = 1")) {
                    assertThat(resultat.next()).isTrue();
                    assertThat(resultat.getString("nom")).isEqualTo("Maman");
                    assertThat(resultat.getBigDecimal("budget")).isEqualByComparingTo("25");
                    assertThat(resultat.getBoolean("termine")).isFalse();
                    assertThat(resultat.wasNull()).isFalse();
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registre);
        }
    }

    @Test
    void persisteEtatTermineDePersonne() {
        ListeAchat liste = listes.saveAndFlush(new ListeAchat("Noel", utilisateur("alice")));
        Personne personne = personnes.saveAndFlush(new Personne("Maman", BigDecimal.ZERO, liste));
        assertThat(personnes.findById(personne.getId()).orElseThrow().isTermine()).isFalse();

        personne.setTermine(true);
        personnes.saveAndFlush(personne);
        assertThat(personnes.findById(personne.getId()).orElseThrow().isTermine()).isTrue();
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
    void persisteUnRemboursementTiersDistinct() {
        ListeAchat liste = listes.saveAndFlush(new ListeAchat("Noel", utilisateur("alice")));
        Personne destinataire = personnes.saveAndFlush(new Personne("Lea", BigDecimal.ZERO, liste));
        Personne tiers = personnes.saveAndFlush(new Personne("Marc", BigDecimal.ZERO, liste));
        Cadeau cadeau = new Cadeau();
        cadeau.setNom("Livre");
        cadeau.setListe(liste);
        cadeau.setPersonne(destinataire);
        cadeau.setPourTiers(true);
        cadeau.setTiers(tiers);
        cadeau.setTiersRembourseLe(LocalDate.of(2026, 11, 20));
        cadeaux.saveAndFlush(cadeau);

        Cadeau relu = cadeaux.findById(cadeau.getId()).orElseThrow();
        assertThat(relu.getTiers().getId()).isEqualTo(tiers.getId());
        assertThat(relu.getPersonne().getId()).isEqualTo(destinataire.getId());
        assertThat(relu.getTiersRembourseLe()).isEqualTo(cadeau.getTiersRembourseLe());
        assertThat(relu.getRembourseLe()).isNull();
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
