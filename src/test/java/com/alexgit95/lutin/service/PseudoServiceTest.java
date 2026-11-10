package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PseudoServiceTest {

    @Autowired PseudoService pseudos;
    @Autowired UtilisateurRepository utilisateurs;

    @BeforeEach
    @AfterEach
    void nettoyer() {
        utilisateurs.deleteAll();
    }

    private static String empreinte(String graine) {
        return String.format("%64s", graine).replace(' ', '0');
    }

    private void refuse(String pseudo, PseudoException.Motif motif) {
        assertThatThrownBy(() -> pseudos.creer(empreinte("a"), pseudo))
                .isInstanceOfSatisfying(PseudoException.class, e -> assertThat(e.getMotif()).isEqualTo(motif));
        assertThat(utilisateurs.count()).isZero();
    }

    @Test
    void unPseudoValideCreeLeCompteRattacheAAdresseHachee() {
        Utilisateur u = pseudos.creer(empreinte("a"), "Alice.Dupont_1");

        assertThat(u.getLogin()).isEqualTo("Alice.Dupont_1");
        assertThat(u.getLoginCle()).isEqualTo("alice.dupont_1");
        assertThat(u.getEmailHmac()).isEqualTo(empreinte("a"));
        assertThat(u.getPasswordHash()).isNull();
        assertThat(utilisateurs.findByEmailHmac(empreinte("a"))).isPresent();
    }

    @Test
    void lesEspacesEnBordureSontIgnores() {
        assertThat(pseudos.creer(empreinte("a"), "  alice  ").getLogin()).isEqualTo("alice");
    }

    @Test
    void lePseudoDoitFaireEntre4Et50Caracteres() {
        refuse("abc", PseudoException.Motif.FORME);
        refuse("", PseudoException.Motif.FORME);
        refuse(null, PseudoException.Motif.FORME);
        refuse("a".repeat(51), PseudoException.Motif.FORME);
        assertThat(pseudos.creer(empreinte("b"), "abcd").getLogin()).isEqualTo("abcd");
        assertThat(pseudos.creer(empreinte("c"), "a".repeat(50)).getLogin()).hasSize(50);
    }

    @Test
    void seulsLettresChiffresPointTiretEtUnderscoreSontAcceptes() {
        for (String pseudo : new String[]{"ali ce", "alice!", "alicé", "ali@ce", "ali/ce", "<b>ali</b>"}) {
            refuse(pseudo, PseudoException.Motif.FORME);
        }
    }

    @Test
    void lesPseudosReservesSontRefusesQuelleQueSoitLaCasse() {
        for (String pseudo : new String[]{"admin", "Admin", "ADMINISTRATEUR", "root", "Lutin", "system", "SUPPORT"}) {
            refuse(pseudo, PseudoException.Motif.RESERVE);
        }
    }

    @Test
    void unPseudoDejaPrisEstRefuseQuelleQueSoitLaCasse() {
        pseudos.creer(empreinte("a"), "Alice");

        assertThatThrownBy(() -> pseudos.creer(empreinte("b"), "aLICE"))
                .isInstanceOfSatisfying(PseudoException.class, e -> assertThat(e.getMotif()).isEqualTo(PseudoException.Motif.PRIS));
        assertThat(utilisateurs.count()).isEqualTo(1);
    }

    @Test
    void unSecondEnvoiPourLeMemeCompteEstIdempotentEtGardeLePseudo() {
        Utilisateur premier = pseudos.creer(empreinte("a"), "alice");

        Utilisateur second = pseudos.creer(empreinte("a"), "autrepseudo");

        assertThat(second.getId()).isEqualTo(premier.getId());
        assertThat(second.getLogin()).isEqualTo("alice");
        assertThat(utilisateurs.count()).isEqualTo(1);
    }

    @Test
    void lePremierCompteEstAdministrateurEtLesSuivantsNeLeSontPas() {
        Utilisateur premier = pseudos.creer(empreinte("a"), "alice");
        Utilisateur second = pseudos.creer(empreinte("b"), "bobby");
        Utilisateur troisieme = pseudos.creer(empreinte("c"), "carol");

        assertThat(premier.isAdmin()).isTrue();
        assertThat(second.isAdmin()).isFalse();
        assertThat(troisieme.isAdmin()).isFalse();
    }

    @Test
    void desCreationsSimultanteesNeDonnentQuUnAdministrateur() throws Exception {
        int nombre = 6;
        ExecutorService executeur = Executors.newFixedThreadPool(nombre);
        CountDownLatch depart = new CountDownLatch(1);
        List<Future<Utilisateur>> resultats = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            String graine = "u" + i;
            String pseudo = "pseudo" + i;
            Callable<Utilisateur> tache = () -> {
                depart.await();
                return pseudos.creer(empreinte(graine), pseudo);
            };
            resultats.add(executeur.submit(tache));
        }
        depart.countDown();
        for (Future<Utilisateur> resultat : resultats) {
            resultat.get();
        }
        executeur.shutdown();

        assertThat(utilisateurs.count()).isEqualTo(nombre);
        assertThat(utilisateurs.findAll().stream().filter(Utilisateur::isAdmin)).hasSize(1);
    }

    @Test
    void deuxUtilisateursQuiChoisissentLeMemePseudoEnMemeTempsNeCreentQuUnCompte() throws Exception {
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);
        List<Future<Object>> resultats = new ArrayList<>();
        for (String graine : new String[]{"x", "y"}) {
            resultats.add(executeur.submit(() -> {
                depart.await();
                try {
                    return pseudos.creer(empreinte(graine), "Alice");
                } catch (PseudoException e) {
                    return e.getMotif();
                }
            }));
        }
        depart.countDown();
        List<Object> sorties = new ArrayList<>();
        for (Future<Object> resultat : resultats) {
            sorties.add(resultat.get());
        }
        executeur.shutdown();

        assertThat(utilisateurs.count()).isEqualTo(1);
        assertThat(sorties).filteredOn(o -> o instanceof PseudoException.Motif)
                .containsExactly(PseudoException.Motif.PRIS);
    }
}
