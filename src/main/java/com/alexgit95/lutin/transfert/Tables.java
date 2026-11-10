package com.alexgit95.lutin.transfert;

import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.TypeAvis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alexgit95.lutin.transfert.TypeColonne.BOOLEEN;
import static com.alexgit95.lutin.transfert.TypeColonne.DATE;
import static com.alexgit95.lutin.transfert.TypeColonne.DECIMAL;
import static com.alexgit95.lutin.transfert.TypeColonne.INSTANT;
import static com.alexgit95.lutin.transfert.TypeColonne.LONG;
import static com.alexgit95.lutin.transfert.TypeColonne.TEXTE;

/**
 * Descripteurs des tables exportées, dans l'ordre des clés étrangères (parents avant enfants).
 * À mettre à jour, avec {@link #VERSION}, à chaque changement du schéma : un test compare ces descripteurs au modèle JPA.
 */
public final class Tables {

    public static final String FORMAT = "lutin-export";
    public static final int VERSION = 2;

    private static final Set<String> STATUTS = noms(Statut.values());
    private static final Set<String> TYPES_AVIS = noms(TypeAvis.values());

    public static final List<TableTransfert> TOUTES = List.of(
            table("utilisateur", "Utilisateur e", true,
                    id(),
                    texte("login", "e.login", true, 50),
                    texte("login_cle", "e.loginCle", true, 50),
                    texte("password_hash", "e.passwordHash", false, 0),
                    texte("email_hmac", "e.emailHmac", false, 64),
                    col("admin_marker", "e.adminMarker", BOOLEEN, false)),
            table("liste_achat", "ListeAchat e", true,
                    id(),
                    texte("nom", "e.nom", true, 100),
                    ref("proprietaire_id", "e.proprietaire.id", "utilisateur")),
            table("personne", "Personne e", true,
                    id(),
                    texte("nom", "e.nom", true, 100),
                    col("budget", "e.budget", DECIMAL, true),
                    ref("liste_id", "e.liste.id", "liste_achat")),
            table("tag", "Tag e", true,
                    id(),
                    texte("nom", "e.nom", true, 50),
                    ref("liste_id", "e.liste.id", "liste_achat")),
            table("cadeau", "Cadeau e", true,
                    id(),
                    texte("nom", "e.nom", true, 255),
                    texte("url", "e.url", false, 2048),
                    col("prix", "e.prix", DECIMAL, false),
                    new Colonne("statut", "e.statut", TEXTE, true, 20, null, STATUTS),
                    col("masque", "e.masque", BOOLEEN, true),
                    col("pour_tiers", "e.pourTiers", BOOLEEN, true),
                    col("rembourse_le", "e.rembourseLe", DATE, false),
                    ref("personne_id", "e.personne.id", "personne"),
                    ref("liste_id", "e.liste.id", "liste_achat")),
            new TableTransfert("cadeau_tag", "Cadeau e join e.tags t", "e.id, t.id", false, List.of(
                    ref("cadeau_id", "e.id", "cadeau"),
                    ref("tag_id", "t.id", "tag"))),
            table("partage", "Partage e", true,
                    id(),
                    ref("liste_id", "e.liste.id", "liste_achat"),
                    ref("lecteur_id", "e.lecteur.id", "utilisateur")),
            table("wishlist", "Wishlist e", true,
                    id(),
                    texte("nom", "e.nom", true, 100),
                    texte("nom_cle", "e.nomCle", true, 100),
                    ref("proprietaire_id", "e.proprietaire.id", "utilisateur"),
                    texte("jeton_public", "e.jetonPublic", false, 64)),
            table("souhait", "Souhait e", true,
                    id(),
                    texte("nom", "e.nom", true, 255),
                    texte("url", "e.url", false, 2048),
                    col("prix", "e.prix", DECIMAL, false),
                    texte("pris_par_nom", "e.prisParNom", false, 100),
                    col("pris_le", "e.prisLe", INSTANT, false),
                    col("pris_par_invite", "e.prisParInvite", BOOLEEN, false),
                    texte("pris_via_login", "e.prisViaLogin", false, 50),
                    ref("wishlist_id", "e.wishlist.id", "wishlist")),
            table("wishlist_partage", "WishlistPartage e", true,
                    id(),
                    ref("wishlist_id", "e.wishlist.id", "wishlist"),
                    ref("lecteur_id", "e.lecteur.id", "utilisateur")),
            table("avis", "Avis e", true,
                    id(),
                    ref("destinataire_id", "e.destinataire.id", "utilisateur"),
                    new Colonne("type", "e.type", TEXTE, true, 30, null, TYPES_AVIS),
                    col("souhait_id", "e.souhaitId", LONG, false),
                    col("wishlist_id", "e.wishlistId", LONG, false),
                    texte("wishlist_nom", "e.wishlistNom", true, 100),
                    texte("article_nom", "e.articleNom", false, 255),
                    texte("pris_par_nom", "e.prisParNom", false, 100),
                    col("nombre", "e.nombre", LONG, false),
                    col("cree_le", "e.creeLe", INSTANT, true)),
            table("alerte_remboursement", "AlerteRemboursement e", true,
                    id(),
                    ref("liste_id", "e.liste.id", "liste_achat"),
                    texte("cadeau_nom", "e.cadeauNom", true, 255),
                    col("montant", "e.montant", DECIMAL, true),
                    col("cree_le", "e.creeLe", INSTANT, true)));

    private Tables() {
    }

    public static TableTransfert parNom(String nom) {
        return TOUTES.stream().filter(t -> t.nom().equals(nom)).findFirst().orElse(null);
    }

    private static TableTransfert table(String nom, String from, boolean avecId, Colonne... colonnes) {
        return new TableTransfert(nom, from, "e.id", avecId, List.of(colonnes));
    }

    private static Colonne id() {
        return new Colonne("id", "e.id", LONG, true, 0, null, null);
    }

    private static Colonne ref(String nom, String chemin, String parent) {
        return new Colonne(nom, chemin, LONG, true, 0, parent, null);
    }

    private static Colonne texte(String nom, String chemin, boolean obligatoire, int longueur) {
        return new Colonne(nom, chemin, TEXTE, obligatoire, longueur, null, null);
    }

    private static Colonne col(String nom, String chemin, TypeColonne type, boolean obligatoire) {
        return new Colonne(nom, chemin, type, obligatoire, 0, null, null);
    }

    private static Set<String> noms(Enum<?>[] valeurs) {
        return Arrays.stream(valeurs).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }
}
