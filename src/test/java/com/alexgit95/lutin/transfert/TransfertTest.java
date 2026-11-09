package com.alexgit95.lutin.transfert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class TransfertTest extends BaseTransfertTest {

    private String avant() {
        return stable(exporter());
    }

    private static String remplacerLePremier(String json, String cible, String remplacement) {
        assertThat(json).contains(cible);
        return json.replaceFirst(Pattern.quote(cible), Matcher.quoteReplacement(remplacement));
    }

    private void refuse(String fichier, String messageAttendu) {
        String etatInitial = avant();

        assertThatThrownBy(() -> importer(fichier)).isInstanceOf(ImportException.class)
                .hasMessageContaining(messageAttendu);
        assertThat(avant()).isEqualTo(etatInitial);
    }

    @Test
    void lExportContientToutesLesTablesDansLOrdreAvecUnEnteteVersionne() {
        String json = stable(exporter());

        assertThat(json).startsWith("{\"format\":\"lutin-export\",\"version\":1,\"mode\":\"local\"")
                .contains("\"versionApplication\":\"").contains("\"exporteLe\":\"\"");
        int position = json.indexOf("\"tables\":{");
        for (TableTransfert table : Tables.TOUTES) {
            int suivante = json.indexOf("\"" + table.nom() + "\":[", position);
            assertThat(suivante).as(table.nom()).isGreaterThan(position);
            position = suivante;
        }
    }

    @Test
    void lExportCompteLesLignesDeChaqueTable() {
        Map<String, Long> compteurs = export.exporter(new java.io.ByteArrayOutputStream(), "alice");

        assertThat(compteurs).containsEntry("utilisateur", utilisateurs.count())
                .containsEntry("liste_achat", listes.count()).containsEntry("personne", personnes.count())
                .containsEntry("tag", tags.count()).containsEntry("cadeau", cadeaux.count())
                .containsEntry("cadeau_tag", 1L).containsEntry("partage", partages.count())
                .containsEntry("wishlist", wishlists.count()).containsEntry("souhait", souhaits.count())
                .containsEntry("wishlist_partage", wishlistPartages.count()).containsEntry("avis", avis.count())
                .containsEntry("alerte_remboursement", alertes.count());
        assertThat(compteurs.values()).allSatisfy(n -> assertThat(n).isPositive());
    }

    @Test
    void uneGrandeTableSurPlusieursLotsEstExporteeSansDoublon() {
        ajouterDesCadeaux(1100);

        String json = stable(exporter());
        String cadeauxJson = json.substring(json.indexOf("\"cadeau\":["), json.indexOf("\"cadeau_tag\":["));
        Set<String> identifiants = new HashSet<>();
        Matcher m = Pattern.compile("\\{\"id\":(\\d+),").matcher(cadeauxJson);
        int total = 0;
        while (m.find()) {
            identifiants.add(m.group(1));
            total++;
        }

        assertThat(total).isEqualTo((int) cadeaux.count()).isGreaterThan(1100);
        assertThat(identifiants).hasSize(total);
    }

    @Test
    void unAllerRetourRestitueLesMemesLignesValeursEtIdentifiants() {
        byte[] sauvegarde = exporter();
        String attendu = stable(sauvegarde);
        // Des modifications faites après la sauvegarde disparaissent à la restauration.
        listesService.creer("alice", "Liste ajoutee apres");
        personnesService.creer(listeId, "alice", "Papa", BigDecimal.TEN);
        cadeaux.deleteAll();
        utilisateurs.findByLogin("bob").ifPresent(u -> { });

        ResumeImport resume = importer(sauvegarde);

        assertThat(stable(exporter())).isEqualTo(attendu);
        assertThat(resume.total()).isPositive();
        assertThat(listes.findAll()).extracting(l -> l.getNom()).containsExactly("Noel");
    }

    @Test
    void lesComptesLesLiensPublicsEtLesHachesSontRestaures() {
        byte[] sauvegarde = exporter();
        wishlistService.rendrePrivee(wishlistId, "alice");

        importer(sauvegarde);

        assertThat(wishlists.findAll().get(0).getJetonPublic()).isEqualTo(jeton);
        assertThat(utilisateurs.findByLogin("alice").orElseThrow().getPasswordHash()).isEqualTo("hash-alice");
        assertThat(utilisateurs.findByLogin("alice").orElseThrow().isAdmin()).isTrue();
    }

    @Test
    void lesCreationsApresImportDepassentLesIdentifiantsImportes() {
        byte[] sauvegarde = exporter();
        long maxListe = listes.findAll().stream().mapToLong(l -> l.getId()).max().orElseThrow();
        long maxCadeau = cadeaux.findAll().stream().mapToLong(c -> c.getId()).max().orElseThrow();

        importer(sauvegarde);
        Long nouvelleListe = listesService.creer("alice", "Nouvelle").getId();
        Long nouveauCadeau = cadeauxService.creer(nouvelleListe, "alice", new com.alexgit95.lutin.service.CadeauCommande(
                "Neuf", null, null, personnesService.creer(nouvelleListe, "alice", "Z", BigDecimal.ZERO).getId(),
                false, false, Set.of())).getId();

        assertThat(nouvelleListe).isGreaterThan(maxListe);
        assertThat(nouveauCadeau).isGreaterThan(maxCadeau);
    }

    @Test
    void unFichierDUneAutreVersionOuUnAutreFormatOuUnAutreModeEstRefuse() {
        String json = stable(exporter());

        refuse(remplacerLePremier(json, "\"version\":1", "\"version\":2"), "Version de format incompatible");
        refuse(remplacerLePremier(json, "\"format\":\"lutin-export\"", "\"format\":\"autre\""), "pas un export Lutin");
        refuse(remplacerLePremier(json, "\"mode\":\"local\"", "\"mode\":\"cloudflare\""), "mode");
    }

    @Test
    void unFichierTronqueOuIllisibleEstRefuse() {
        String json = stable(exporter());

        refuse(json.substring(0, json.length() / 2), "illisible ou incomplet");
        refuse(json.substring(0, json.length() - 1), "illisible ou incomplet");
        refuse("n'importe quoi", "illisible ou incomplet");
        refuse("", "objet JSON est attendu");
        refuse(json + "{}", "contenu après la fin");
    }

    @Test
    void uneTableOuUneColonneInconnueEstRefusee() {
        String json = stable(exporter());

        refuse(remplacerLePremier(json, "\"tag\":[", "\"tagx\":["), "table inconnue");
        refuse(remplacerLePremier(json, "\"login_cle\":", "\"login_clx\":"), "colonne inconnue");
        refuse(remplacerLePremier(json, "\"login_cle\":\"alice\",", ""), "colonne absente");
    }

    @Test
    void uneReferenceManquanteOuUnIdentifiantEnDoubleEstRefuse() {
        String json = stable(exporter());
        Matcher proprietaire = Pattern.compile("\"proprietaire_id\":(\\d+)").matcher(json);
        assertThat(proprietaire.find()).isTrue();

        refuse(remplacerLePremier(json, proprietaire.group(), "\"proprietaire_id\":999999"), "référence vers une ligne absente");
        Matcher id = Pattern.compile("\\{\"id\":(\\d+),\"login\":\"bob\"").matcher(json);
        assertThat(id.find()).isTrue();
        Matcher idAlice = Pattern.compile("\\{\"id\":(\\d+),\"login\":\"alice\"").matcher(json);
        assertThat(idAlice.find()).isTrue();
        refuse(remplacerLePremier(json, id.group(), "{\"id\":" + idAlice.group(1) + ",\"login\":\"bob\""), "identifiant en double");
    }

    @Test
    void unTypeUneLongueurOuUneEnumerationInvalideEstRefuse() {
        String json = stable(exporter());

        refuse(remplacerLePremier(json, "\"masque\":false", "\"masque\":\"oui\""), "type invalide");
        refuse(remplacerLePremier(json, "\"nom\":\"Maman\"", "\"nom\":\"" + "x".repeat(101) + "\""), "valeur trop longue");
        refuse(remplacerLePremier(json, "\"statut\":\"COMMANDE\"", "\"statut\":\"BIDON\""), "valeur non autorisée");
        Matcher budget = Pattern.compile("\"budget\":\"[^\"]*\"").matcher(json);
        assertThat(budget.find()).isTrue();
        refuse(remplacerLePremier(json, budget.group(), "\"budget\":\"beaucoup\""), "format invalide");
        refuse(remplacerLePremier(json, "\"cree_le\":\"", "\"cree_le\":\"hier"), "format invalide");
        refuse(remplacerLePremier(json, "\"nom\":\"Maman\"", "\"nom\":null"), "valeur obligatoire");
    }

    @Test
    void sansLeCompteAdministrateurCourantDansLeFichierLImportEstRefuse() {
        String json = stable(exporter());
        String sansRole = remplacerLePremier(json, "\"admin_marker\":true", "\"admin_marker\":null");
        String autreLogin = remplacerLePremier(remplacerLePremier(json, "\"login\":\"alice\"", "\"login\":\"alicex\""),
                "\"login_cle\":\"alice\"", "\"login_cle\":\"alicex\"");

        // En mode local la correspondance porte sur le login ; le rôle est exigé dans tous les cas.
        refuse(sansRole, "ne contient pas votre compte administrateur");
        refuse(autreLogin, "ne contient pas votre compte administrateur");
    }

    @Test
    void uneErreurPendantLEcritureAnnuleAussiLaSuppression() {
        byte[] sauvegarde = exporter();
        String attendu = stable(sauvegarde);
        int[] ouvertures = {0};

        assertThatThrownBy(() -> importService.importer(() -> {
            ouvertures[0]++;
            return ouvertures[0] == 1 ? new ByteArrayInputStream(sauvegarde) : new EchecEnMilieu(sauvegarde);
        }, alice())).isInstanceOf(ImportException.class);

        assertThat(stable(exporter())).isEqualTo(attendu);
    }

    @Test
    void unSecondImportSimultaneEstRefuseSansGenerLePremier() throws Exception {
        byte[] sauvegarde = exporter();
        CountDownLatch entre = new CountDownLatch(1);
        CountDownLatch libere = new CountDownLatch(1);
        ExecutorService executeur = Executors.newSingleThreadExecutor();

        Future<ResumeImport> premier = executeur.submit(() -> importService.importer(() -> new Bloquant(sauvegarde, entre, libere), alice()));
        assertThat(entre.await(10, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> importer(sauvegarde)).isInstanceOf(ImportEnCoursException.class);

        libere.countDown();
        assertThat(premier.get(30, TimeUnit.SECONDS).total()).isPositive();
        executeur.shutdown();
    }

    @Test
    void ExportEtImportNeJournalisentAucuneDonnee(CapturedOutput sortie) {
        byte[] sauvegarde = exporter();
        importer(sauvegarde);

        assertThat(sortie.getAll()).contains("Export de la base par alice").contains("Import de la base par alice")
                .doesNotContain("hash-alice").doesNotContain(jeton).doesNotContain("Maman")
                .doesNotContain("shop.example");
    }

    /** Flux qui échoue après la moitié du contenu, pour simuler une erreur pendant la seconde passe. */
    private static final class EchecEnMilieu extends InputStream {
        private final ByteArrayInputStream interne;
        private int restant;

        EchecEnMilieu(byte[] contenu) {
            this.interne = new ByteArrayInputStream(contenu);
            this.restant = contenu.length / 2;
        }

        @Override
        public int read() throws IOException {
            if (restant-- <= 0) {
                throw new IOException("coupure");
            }
            return interne.read();
        }
    }

    /** Flux dont la première lecture signale qu'il est ouvert puis attend d'être libéré. */
    private static final class Bloquant extends InputStream {
        private final ByteArrayInputStream interne;
        private final CountDownLatch entre;
        private final CountDownLatch libere;
        private boolean signale;

        Bloquant(byte[] contenu, CountDownLatch entre, CountDownLatch libere) {
            this.interne = new ByteArrayInputStream(contenu);
            this.entre = entre;
            this.libere = libere;
        }

        @Override
        public int read() throws IOException {
            if (!signale) {
                signale = true;
                entre.countDown();
                try {
                    libere.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
            return interne.read();
        }
    }
}
