package com.alexgit95.lutin.transfert;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Lit un fichier d'export en flux et le restitue ligne à ligne à un {@link Visiteur}. Les valeurs sont typées selon les
 * descripteurs ; toute anomalie lève une {@link ImportException} sans reprendre la valeur fautive.
 */
public final class LecteurExport {

    /** Reçoit le contenu au fil de la lecture ; les valeurs d'une ligne suivent l'ordre des colonnes du descripteur. */
    public interface Visiteur {
        void entete(String mode);

        void debutTable(TableTransfert table);

        void ligne(TableTransfert table, long numero, Object[] valeurs);

        void finTable(TableTransfert table);
    }

    private static final JsonFactory FABRIQUE = new JsonFactory();
    private static final List<TableTransfert> TABLES = Tables.TOUTES;

    private LecteurExport() {
    }

    public static void lire(InputStream flux, Visiteur visiteur) throws IOException {
        try (JsonParser parseur = FABRIQUE.createParser(flux)) {
            lireDocument(parseur, visiteur);
        } catch (JacksonException e) {
            throw new ImportException("Fichier illisible ou incomplet : ce n'est pas un export JSON valide");
        }
    }

    private static void lireDocument(JsonParser p, Visiteur visiteur) {
        if (p.nextToken() != JsonToken.START_OBJECT) {
            throw new ImportException("Fichier invalide : un objet JSON est attendu");
        }
        String format = null;
        Long version = null;
        String mode = null;
        boolean tablesLues = false;
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            String nom = p.currentName();
            JsonToken valeur = p.nextToken();
            switch (nom) {
                case "format" -> format = texte(valeur, p, "format");
                case "version" -> version = entier(valeur, p, "version");
                case "mode" -> mode = texte(valeur, p, "mode");
                case "versionApplication", "exporteLe" -> texte(valeur, p, nom);
                case "tables" -> {
                    verifierEntete(format, version, mode);
                    visiteur.entete(mode);
                    lireTables(p, valeur, visiteur);
                    tablesLues = true;
                }
                default -> throw new ImportException("Fichier invalide : champ inattendu dans l'en-tête");
            }
        }
        if (p.currentToken() != JsonToken.END_OBJECT) {
            throw new ImportException("Fichier illisible ou incomplet");
        }
        if (p.nextToken() != null) {
            throw new ImportException("Fichier invalide : contenu après la fin de l'export");
        }
        if (!tablesLues) {
            throw new ImportException("Fichier invalide : aucune table");
        }
    }

    private static void verifierEntete(String format, Long version, String mode) {
        if (!Tables.FORMAT.equals(format)) {
            throw new ImportException("Fichier invalide : ce n'est pas un export Lutin");
        }
        if (version == null || version != Tables.VERSION) {
            throw new ImportException("Version de format incompatible : le fichier doit être exporté par la même version de l'application");
        }
        if (mode == null) {
            throw new ImportException("Fichier invalide : mode d'accès absent de l'en-tête");
        }
    }

    private static void lireTables(JsonParser p, JsonToken debut, Visiteur visiteur) {
        if (debut != JsonToken.START_OBJECT) {
            throw new ImportException("Fichier invalide : les tables doivent former un objet");
        }
        int attendue = 0;
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            if (attendue >= TABLES.size() || !TABLES.get(attendue).nom().equals(p.currentName())) {
                throw new ImportException("Fichier invalide : table inconnue ou hors de l'ordre attendu");
            }
            TableTransfert table = TABLES.get(attendue++);
            if (p.nextToken() != JsonToken.START_ARRAY) {
                throw new ImportException("Table « " + table.nom() + " » : un tableau de lignes est attendu");
            }
            visiteur.debutTable(table);
            long numero = 0;
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.currentToken() != JsonToken.START_OBJECT) {
                    throw new ImportException("Table « " + table.nom() + " » : une ligne doit être un objet");
                }
                visiteur.ligne(table, ++numero, lireLigne(p, table, numero));
            }
            visiteur.finTable(table);
        }
        if (attendue != TABLES.size()) {
            throw new ImportException("Fichier invalide : des tables sont absentes");
        }
    }

    private static Object[] lireLigne(JsonParser p, TableTransfert table, long numero) {
        int taille = table.colonnes().size();
        Object[] valeurs = new Object[taille];
        boolean[] vues = new boolean[taille];
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            int index = table.indexDe(p.currentName());
            if (index < 0) {
                throw erreur(table, numero, null, "colonne inconnue");
            }
            Colonne colonne = table.colonnes().get(index);
            if (vues[index]) {
                throw erreur(table, numero, colonne, "colonne en double");
            }
            vues[index] = true;
            valeurs[index] = convertir(p, p.nextToken(), table, numero, colonne);
        }
        for (int i = 0; i < taille; i++) {
            if (!vues[i]) {
                throw erreur(table, numero, table.colonnes().get(i), "colonne absente");
            }
        }
        return valeurs;
    }

    private static Object convertir(JsonParser p, JsonToken jeton, TableTransfert table, long numero, Colonne c) {
        if (jeton == JsonToken.VALUE_NULL) {
            if (c.obligatoire()) {
                throw erreur(table, numero, c, "valeur obligatoire");
            }
            return null;
        }
        try {
            return switch (c.type()) {
                case LONG -> {
                    exiger(jeton == JsonToken.VALUE_NUMBER_INT, table, numero, c);
                    yield p.getLongValue();
                }
                case BOOLEEN -> {
                    exiger(jeton == JsonToken.VALUE_TRUE || jeton == JsonToken.VALUE_FALSE, table, numero, c);
                    yield jeton == JsonToken.VALUE_TRUE;
                }
                case TEXTE -> {
                    exiger(jeton == JsonToken.VALUE_STRING, table, numero, c);
                    String texte = p.getString();
                    if (c.longueur() > 0 && texte.length() > c.longueur()) {
                        throw erreur(table, numero, c, "valeur trop longue");
                    }
                    if (c.valeurs() != null && !c.valeurs().contains(texte)) {
                        throw erreur(table, numero, c, "valeur non autorisée");
                    }
                    yield texte;
                }
                case DECIMAL -> {
                    exiger(jeton == JsonToken.VALUE_STRING, table, numero, c);
                    BigDecimal decimal = new BigDecimal(p.getString());
                    if (decimal.precision() - decimal.scale() > 10 || decimal.scale() > 2) {
                        throw erreur(table, numero, c, "montant hors limites");
                    }
                    yield decimal;
                }
                case DATE -> {
                    exiger(jeton == JsonToken.VALUE_STRING, table, numero, c);
                    yield LocalDate.parse(p.getString());
                }
                case INSTANT -> {
                    exiger(jeton == JsonToken.VALUE_STRING, table, numero, c);
                    yield Instant.parse(p.getString());
                }
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw erreur(table, numero, c, "format invalide");
        }
    }

    private static void exiger(boolean condition, TableTransfert table, long numero, Colonne colonne) {
        if (!condition) {
            throw erreur(table, numero, colonne, "type invalide");
        }
    }

    private static ImportException erreur(TableTransfert table, long numero, Colonne colonne, String motif) {
        return new ImportException("Table « " + table.nom() + " », ligne " + numero
                + (colonne == null ? "" : ", colonne « " + colonne.nom() + " »") + " : " + motif);
    }

    private static String texte(JsonToken jeton, JsonParser p, String champ) {
        if (jeton != JsonToken.VALUE_STRING) {
            throw new ImportException("Fichier invalide : « " + champ + " » doit être un texte");
        }
        return p.getString();
    }

    private static long entier(JsonToken jeton, JsonParser p, String champ) {
        if (jeton != JsonToken.VALUE_NUMBER_INT) {
            throw new ImportException("Fichier invalide : « " + champ + " » doit être un entier");
        }
        return p.getLongValue();
    }
}
