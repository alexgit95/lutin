package com.alexgit95.lutin.transfert;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Tag;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.AlerteRemboursementRepository;
import com.alexgit95.lutin.repository.ApiKeyRepository;
import com.alexgit95.lutin.repository.AvisRepository;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PartageRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.SouhaitRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import com.alexgit95.lutin.repository.WishlistPartageRepository;
import com.alexgit95.lutin.repository.WishlistRepository;
import com.alexgit95.lutin.security.EmailHmacService;
import com.alexgit95.lutin.service.CadeauCommande;
import com.alexgit95.lutin.service.CadeauService;
import com.alexgit95.lutin.service.CleApiService;
import com.alexgit95.lutin.service.ListeAchatService;
import com.alexgit95.lutin.service.PartageService;
import com.alexgit95.lutin.service.PersonneService;
import com.alexgit95.lutin.service.SouhaitCommande;
import com.alexgit95.lutin.service.SouhaitService;
import com.alexgit95.lutin.service.WishlistPartageService;
import com.alexgit95.lutin.service.WishlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Jeu de données couvrant les 13 tables, partagé par les tests de transfert (modes local et cloudflare). */
abstract class BaseTransfertTest {

    @Autowired ExportService export;
    @Autowired ImportService importService;
    @Autowired ListeAchatService listesService;
    @Autowired PersonneService personnesService;
    @Autowired CadeauService cadeauxService;
    @Autowired PartageService partagesService;
    @Autowired WishlistService wishlistService;
    @Autowired SouhaitService souhaitService;
    @Autowired WishlistPartageService wishlistPartageService;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired ListeAchatRepository listes;
    @Autowired PersonneRepository personnes;
    @Autowired CadeauRepository cadeaux;
    @Autowired TagRepository tags;
    @Autowired PartageRepository partages;
    @Autowired WishlistRepository wishlists;
    @Autowired SouhaitRepository souhaits;
    @Autowired WishlistPartageRepository wishlistPartages;
    @Autowired AvisRepository avis;
    @Autowired AlerteRemboursementRepository alertes;
    @Autowired ApiKeyRepository clesApi;
    @Autowired CleApiService cleApiService;
    @Autowired(required = false) EmailHmacService hmac;

    protected Long listeId;
    protected Long mamanId;
    protected Long wishlistId;
    protected String jeton;
    protected String cleApi;

    @BeforeEach
    void preparerLesDonnees() {
        nettoyerLesDonnees();
        creerUtilisateur("alice", true);
        creerUtilisateur("bob", false);
        creerUtilisateur("carol", false);

        listeId = listesService.creer("alice", "Noel").getId();
        mamanId = personnesService.creer(listeId, "alice", "Maman", new BigDecimal("500")).getId();
        Long tagId = tags.saveAndFlush(new Tag("noel", listes.findById(listeId).orElseThrow())).getId();
        Long montre = cadeauxService.creer(listeId, "alice", new CadeauCommande("Montre", "https://shop.example/montre",
                new BigDecimal("80.50"), mamanId, false, false, Set.of(tagId))).getId();
        cadeauxService.changerStatut(listeId, montre, "alice", Statut.COMMANDE);
        cadeauxService.basculerRemboursement(listeId, montre, "alice");
        cadeauxService.creer(listeId, "alice", new CadeauCommande("Idee secrete", null, null, mamanId, true, true, Set.of()));
        Long vendu = cadeauxService.creer(listeId, "alice", new CadeauCommande("Livre", null, new BigDecimal("12"),
                mamanId, false, false, Set.of())).getId();
        cadeauxService.changerStatut(listeId, vendu, "alice", Statut.COMMANDE);
        cadeauxService.basculerRemboursement(listeId, vendu, "alice");
        cadeauxService.changerStatut(listeId, vendu, "alice", Statut.A_ACHETER);
        partagesService.partager(listeId, "alice", "bob");

        wishlistId = wishlistService.creer("alice", "Anniversaire").getId();
        jeton = wishlistService.rendrePublique(wishlistId, "alice");
        Long casque = souhaitService.creer(wishlistId, "alice",
                new SouhaitCommande("Casque audio", "https://shop.example/casque", new BigDecimal("99.99"))).getId();
        Long livre = souhaitService.creer(wishlistId, "alice", new SouhaitCommande("Livre sans prix", null, null)).getId();
        wishlistPartageService.partager(wishlistId, "alice", "bob");
        souhaitService.prendre(wishlistId, casque, "bob");
        souhaitService.prendrePour(wishlistId, livre, "bob", "Tante Marie");
        souhaitService.modifier(wishlistId, casque, "alice",
                new SouhaitCommande("Casque audio v2", "https://shop.example/casque", new BigDecimal("89.99")));
        cleApi = cleApiService.generer("nas-backup").cle();
        cleApiService.authentifier(cleApi);
    }

    @AfterEach
    void nettoyerLesDonnees() {
        avis.deleteAll();
        souhaits.deleteAll();
        wishlistPartages.deleteAll();
        wishlists.deleteAll();
        alertes.deleteAll();
        clesApi.deleteAll();
        cadeaux.deleteAll();
        partages.deleteAll();
        tags.deleteAll();
        personnes.deleteAll();
        listes.deleteAll();
        utilisateurs.deleteAll();
    }

    protected void creerUtilisateur(String login, boolean admin) {
        Utilisateur u = new Utilisateur(login, hmac == null ? "hash-" + login : null);
        if (hmac != null) {
            u.setEmailHmac(hmac.hacher(login + "@example.org"));
        }
        u.setAdmin(admin);
        utilisateurs.saveAndFlush(u);
    }

    protected IdentiteAdmin alice() {
        return new IdentiteAdmin("alice", hmac == null ? null : hmac.hacher("alice@example.org"));
    }

    protected byte[] exporter() {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        export.exporter(sortie, "alice");
        return sortie.toByteArray();
    }

    /** L'export sans sa date, pour comparer deux exports du même contenu. */
    protected static String stable(byte[] export) {
        return new String(export, StandardCharsets.UTF_8).replaceAll("\"exporteLe\":\"[^\"]*\"", "\"exporteLe\":\"\"");
    }

    protected ResumeImport importer(byte[] contenu) {
        return importService.importer(() -> new ByteArrayInputStream(contenu), alice());
    }

    protected ResumeImport importer(String contenu) {
        return importer(contenu.getBytes(StandardCharsets.UTF_8));
    }

    /** Ajoute des cadeaux directement par le dépôt, pour dépasser la taille d'un lot d'export. */
    protected void ajouterDesCadeaux(int nombre) {
        List<Cadeau> lot = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            Cadeau c = new Cadeau();
            c.setNom("Cadeau en masse " + i);
            c.setPrix(new BigDecimal("1.00"));
            c.setStatut(Statut.A_ACHETER);
            c.setPersonne(personnes.getReferenceById(mamanId));
            c.setListe(listes.getReferenceById(listeId));
            lot.add(c);
        }
        cadeaux.saveAll(lot);
    }
}
