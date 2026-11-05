package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Tag;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CadeauService {

    private static final BigDecimal PRIX_MAX = new BigDecimal("10000000000");

    private final CadeauRepository cadeaux;
    private final PersonneRepository personnes;
    private final TagRepository tags;
    private final AccesListeService acces;
    private final UrlTitleService titres;

    public CadeauService(CadeauRepository cadeaux, PersonneRepository personnes, TagRepository tags,
                         AccesListeService acces, UrlTitleService titres) {
        this.cadeaux = cadeaux;
        this.personnes = personnes;
        this.tags = tags;
        this.acces = acces;
        this.titres = titres;
    }

    /** Hors transaction : le réseau ne doit pas retenir une connexion à la base. */
    @Transactional(propagation = Propagation.SUPPORTS)
    public CadeauCommande completerNom(CadeauCommande commande) {
        boolean sansNom = commande.nom() == null || commande.nom().isBlank();
        String url = commande.url() == null ? "" : commande.url().trim();
        if (!sansNom || url.isEmpty() || !UrlTitleService.estUrlHttpValide(url)) {
            return commande;
        }
        return titres.titre(url)
                .map(titre -> new CadeauCommande(titre, commande.url(), commande.prix(), commande.personneId(),
                        commande.masque(), commande.pourTiers(), commande.tagIds()))
                .orElse(commande);
    }

    public Cadeau creer(Long listeId, String login, CadeauCommande commande) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        Cadeau cadeau = new Cadeau();
        cadeau.setListe(liste);
        appliquer(cadeau, listeId, commande);
        return cadeaux.save(cadeau);
    }

    @Transactional(readOnly = true)
    public CadeauCommande obtenir(Long listeId, Long cadeauId, String login) {
        acces.proprietaire(listeId, login);
        Cadeau c = dansLaListe(listeId, cadeauId);
        Set<Long> tagIds = c.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        return new CadeauCommande(c.getNom(), c.getUrl(), c.getPrix(), c.getPersonne().getId(),
                c.isMasque(), c.isPourTiers(), tagIds);
    }

    public void modifier(Long listeId, Long cadeauId, String login, CadeauCommande commande) {
        acces.proprietaire(listeId, login);
        appliquer(dansLaListe(listeId, cadeauId), listeId, commande);
    }

    public void changerStatut(Long listeId, Long cadeauId, String login, Statut statut) {
        acces.proprietaire(listeId, login);
        if (statut == null) {
            throw new RequeteInvalideException("Statut invalide");
        }
        dansLaListe(listeId, cadeauId).setStatut(statut);
    }

    public void supprimer(Long listeId, Long cadeauId, String login) {
        acces.proprietaire(listeId, login);
        cadeaux.delete(dansLaListe(listeId, cadeauId));
    }

    private void appliquer(Cadeau cadeau, Long listeId, CadeauCommande commande) {
        String url = commande.url() == null ? "" : commande.url().trim();
        if (url.length() > 2048 || (!url.isEmpty() && !UrlTitleService.estUrlHttpValide(url))) {
            throw new RequeteInvalideException("L'URL doit commencer par http:// ou https://");
        }
        cadeau.setNom(resoudreNom(commande.nom(), url));
        cadeau.setUrl(url.isEmpty() ? null : url);
        cadeau.setPrix(prixValide(commande.prix()));
        cadeau.setMasque(commande.masque());
        cadeau.setPourTiers(commande.pourTiers());
        cadeau.setPersonne(personneDeLaListe(listeId, commande.personneId()));
        cadeau.setTags(tagsDeLaListe(listeId, commande.tagIds()));
    }

    private String resoudreNom(String nom, String url) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty()) {
            throw new RequeteInvalideException(url.isEmpty()
                    ? "Le nom est obligatoire"
                    : "Impossible de déduire le nom depuis l'URL, saisissez-le");
        }
        if (propre.length() > 255) {
            throw new RequeteInvalideException("Le nom est trop long (255 caractères maximum)");
        }
        return propre;
    }

    private BigDecimal prixValide(BigDecimal prix) {
        if (prix == null) {
            return null;
        }
        BigDecimal arrondi = prix.setScale(2, RoundingMode.HALF_UP);
        if (arrondi.signum() < 0 || arrondi.compareTo(PRIX_MAX) >= 0) {
            throw new RequeteInvalideException("Le prix doit être positif");
        }
        return arrondi;
    }

    private Personne personneDeLaListe(Long listeId, Long personneId) {
        Personne personne = personneId == null ? null : personnes.findById(personneId).orElse(null);
        if (personne == null || !personne.getListe().getId().equals(listeId)) {
            throw new RequeteInvalideException("Choisissez un destinataire de cette liste");
        }
        return personne;
    }

    private Set<Tag> tagsDeLaListe(Long listeId, Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> trouves = tags.findByListeIdAndIdIn(listeId, tagIds);
        if (trouves.size() != tagIds.size()) {
            throw new RequeteInvalideException("Tag inconnu pour cette liste");
        }
        return new HashSet<>(trouves);
    }

    private Cadeau dansLaListe(Long listeId, Long cadeauId) {
        Cadeau cadeau = cadeaux.findById(cadeauId)
                .orElseThrow(() -> new ElementIntrouvableException("Cadeau introuvable"));
        if (!cadeau.getListe().getId().equals(listeId)) {
            throw new ElementIntrouvableException("Cadeau introuvable");
        }
        return cadeau;
    }
}
