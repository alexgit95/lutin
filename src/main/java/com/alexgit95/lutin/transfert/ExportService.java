package com.alexgit95.lutin.transfert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export de toute la base en un document JSON écrit en flux, table par table et par lots. */
@Service
public class ExportService {

    private static final Logger JOURNAL = LoggerFactory.getLogger(ExportService.class);
    private static final int LOT = 500;
    private static final JsonFactory FABRIQUE = new JsonFactory();

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate transaction;
    private final String mode;
    private final String versionApplication;

    public ExportService(PlatformTransactionManager gestionnaire, @Value("${lutin.acces.mode:local}") String mode,
                         ObjectProvider<BuildProperties> build) {
        this.transaction = new TransactionTemplate(gestionnaire);
        this.transaction.setReadOnly(true);
        this.mode = mode;
        BuildProperties proprietes = build.getIfAvailable();
        this.versionApplication = proprietes == null ? "inconnue" : proprietes.getVersion();
    }

    /** Écrit l'export dans le flux et renvoie le nombre de lignes par table ; le contenu n'est jamais journalisé. */
    public Map<String, Long> exporter(OutputStream sortie, String administrateur) {
        Map<String, Long> compteurs = transaction.execute(statut -> ecrire(sortie));
        JOURNAL.info("Export de la base par {} : {} lignes", administrateur,
                compteurs.values().stream().mapToLong(Long::longValue).sum());
        return compteurs;
    }

    private Map<String, Long> ecrire(OutputStream sortie) {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        try (JsonGenerator g = FABRIQUE.createGenerator(sortie)) {
            g.writeStartObject();
            g.writeName("format");
            g.writeString(Tables.FORMAT);
            g.writeName("version");
            g.writeNumber(Tables.VERSION);
            g.writeName("mode");
            g.writeString(mode);
            g.writeName("versionApplication");
            g.writeString(versionApplication);
            g.writeName("exporteLe");
            g.writeString(Instant.now().toString());
            g.writeName("tables");
            g.writeStartObject();
            for (TableTransfert table : Tables.TOUTES) {
                g.writeName(table.nom());
                g.writeStartArray();
                compteurs.put(table.nom(), ecrireLignes(g, table));
                g.writeEndArray();
            }
            g.writeEndObject();
            g.writeEndObject();
        }
        return compteurs;
    }

    private long ecrireLignes(JsonGenerator g, TableTransfert table) {
        long total = 0;
        while (true) {
            List<Object[]> lot = em.createQuery(table.requete(), Object[].class)
                    .setFirstResult((int) total).setMaxResults(LOT).getResultList();
            for (Object[] ligne : lot) {
                g.writeStartObject();
                for (int i = 0; i < table.colonnes().size(); i++) {
                    g.writeName(table.colonnes().get(i).nom());
                    ecrireValeur(g, table.colonnes().get(i), ligne[i]);
                }
                g.writeEndObject();
            }
            total += lot.size();
            em.clear();
            if (lot.size() < LOT) {
                return total;
            }
        }
    }

    private static void ecrireValeur(JsonGenerator g, Colonne colonne, Object valeur) {
        if (valeur == null) {
            g.writeNull();
            return;
        }
        switch (colonne.type()) {
            case LONG -> g.writeNumber(((Number) valeur).longValue());
            case BOOLEEN -> g.writeBoolean((Boolean) valeur);
            case DECIMAL -> g.writeString(((BigDecimal) valeur).toPlainString());
            case DATE, INSTANT -> g.writeString(valeur.toString());
            case TEXTE -> g.writeString(valeur instanceof Enum<?> e ? e.name() : valeur.toString());
        }
    }
}
