package com.alexgit95.lutin.transfert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Import par remplacement total : le fichier est lu une première fois sans rien écrire (validation), puis une seconde
 * fois dans une transaction unique qui vide la base et réinsère les lignes avec leurs identifiants.
 */
@Service
public class ImportService {

    /** Ouvre le fichier envoyé ; appelé deux fois, une par passe. */
    @FunctionalInterface
    public interface SourceFichier {
        InputStream ouvrir() throws IOException;
    }

    private static final Logger JOURNAL = LoggerFactory.getLogger(ImportService.class);

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate transaction;
    private final String mode;
    private final ReentrantLock verrou = new ReentrantLock();

    public ImportService(PlatformTransactionManager gestionnaire, @Value("${lutin.acces.mode:local}") String mode) {
        this.transaction = new TransactionTemplate(gestionnaire);
        this.mode = mode;
    }

    public ResumeImport importer(SourceFichier source, IdentiteAdmin administrateur) {
        if (!verrou.tryLock()) {
            throw new ImportEnCoursException();
        }
        try {
            Validation validation = new Validation(administrateur);
            try (InputStream flux = source.ouvrir()) {
                LecteurExport.lire(flux, validation);
            }
            validation.verifierAdministrateur();

            ResumeImport resume = transaction.execute(statut -> {
                try (InputStream flux = source.ouvrir()) {
                    effacerTout();
                    Ecriture ecriture = new Ecriture();
                    LecteurExport.lire(flux, ecriture);
                    recalerIdentifiants();
                    return new ResumeImport(ecriture.compteurs);
                } catch (IOException e) {
                    throw new ImportException("Fichier illisible");
                }
            });
            JOURNAL.info("Import de la base par {} : {} lignes", administrateur.login(), resume.total());
            return resume;
        } catch (IOException e) {
            throw new ImportException("Fichier illisible");
        } finally {
            verrou.unlock();
        }
    }

    private void effacerTout() {
        List<TableTransfert> tables = Tables.TOUTES;
        for (int i = tables.size() - 1; i >= 0; i--) {
            em.createNativeQuery("delete from " + tables.get(i).nom()).executeUpdate();
        }
        em.clear();
    }

    /** PostgreSQL : les séquences d'identité ne suivent pas les identifiants insérés explicitement. */
    private void recalerIdentifiants() {
        String moteur = em.unwrap(Session.class).doReturningWork(c -> c.getMetaData().getDatabaseProductName());
        if (!"PostgreSQL".equalsIgnoreCase(moteur)) {
            return;
        }
        for (TableTransfert table : Tables.TOUTES) {
            if (table.avecId()) {
                em.createNativeQuery("select setval(pg_get_serial_sequence('" + table.nom() + "', 'id'), "
                        + "coalesce(max(id), 1), max(id) is not null) from " + table.nom()).getSingleResult();
            }
        }
    }

    /** Première passe : contrôle le fichier sans écrire, collecte les identifiants et cherche le compte administrateur. */
    private final class Validation implements LecteurExport.Visiteur {

        private final IdentiteAdmin administrateur;
        private final Map<String, Set<Long>> identifiants = new HashMap<>();
        private boolean administrateurTrouve;

        Validation(IdentiteAdmin administrateur) {
            this.administrateur = administrateur;
        }

        @Override
        public void entete(String modeFichier) {
            if (!mode.equals(modeFichier)) {
                throw new ImportException("Fichier exporté en mode « " + modeFichier
                        + " » : cette instance est en mode « " + mode + " »");
            }
        }

        @Override
        public void debutTable(TableTransfert table) {
            identifiants.put(table.nom(), new HashSet<>());
        }

        @Override
        public void ligne(TableTransfert table, long numero, Object[] valeurs) {
            for (int i = 0; i < valeurs.length; i++) {
                Colonne colonne = table.colonnes().get(i);
                if (colonne.parent() != null && valeurs[i] != null
                        && !identifiants.get(colonne.parent()).contains((Long) valeurs[i])) {
                    throw new ImportException("Table « " + table.nom() + " », ligne " + numero
                            + ", colonne « " + colonne.nom() + " » : référence vers une ligne absente");
                }
            }
            if (table.avecId() && !identifiants.get(table.nom()).add((Long) valeurs[0])) {
                throw new ImportException("Table « " + table.nom() + " », ligne " + numero + " : identifiant en double");
            }
            if ("utilisateur".equals(table.nom())) {
                controlerCompte(table, valeurs);
            }
        }

        private void controlerCompte(TableTransfert table, Object[] valeurs) {
            boolean admin = valeurs[table.indexDe("admin_marker")] != null;
            boolean correspond = administrateur.emailHmac() != null
                    ? administrateur.emailHmac().equals(valeurs[table.indexDe("email_hmac")])
                    : administrateur.login().equals(valeurs[table.indexDe("login")]);
            if (admin && correspond) {
                administrateurTrouve = true;
            }
        }

        @Override
        public void finTable(TableTransfert table) {
        }

        void verifierAdministrateur() {
            if (!administrateurTrouve) {
                throw new ImportException("Ce fichier ne contient pas votre compte administrateur : import refusé");
            }
        }
    }

    /** Seconde passe : insère chaque ligne avec ses identifiants, valeurs liées par leur type Java. */
    private final class Ecriture implements LecteurExport.Visiteur {

        private final Map<String, Long> compteurs = new LinkedHashMap<>();

        @Override
        public void entete(String modeFichier) {
        }

        @Override
        public void debutTable(TableTransfert table) {
            compteurs.put(table.nom(), 0L);
        }

        @Override
        public void ligne(TableTransfert table, long numero, Object[] valeurs) {
            Query requete = em.createNativeQuery(table.insertion());
            for (int i = 0; i < valeurs.length; i++) {
                requete.setParameter("p" + i, valeurs[i]);
            }
            requete.executeUpdate();
            compteurs.merge(table.nom(), 1L, Long::sum);
        }

        @Override
        public void finTable(TableTransfert table) {
        }
    }
}
