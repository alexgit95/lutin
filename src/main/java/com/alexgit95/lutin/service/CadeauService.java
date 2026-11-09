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
import java.time.LocalDate;
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
    private final AlerteRemboursementService alertes;
    private final DateDuJour dateDuJour;

    public record ResultatRemboursement(int nombre, BigDecimal somme) {
    }

    public CadeauService(CadeauRepository cadeaux, PersonneRepository personnes, TagRepository tags,
                         AccesListeService acces, UrlTitleService titres, AlerteRemboursementService alertes,
                         DateDuJour dateDuJour) {
        this.cadeaux = cadeaux;
        this.personnes = personnes;
        this.tags = tags;
        this.acces = acces;
        this.titres = titres;
        this.alertes = alertes;
        this.dateDuJour = dateDuJour;
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
        Cadeau cadeau = dansLaListe(listeId, cadeauId);
        if (cadeau.getRembourseLe() != null) {
            verrouillerPrix(cadeau, commande);
        }
        appliquer(cadeau, listeId, commande);
        // Un cadeau remboursé qui n'est plus remboursable (pour un tiers) rend le virement inutile.
        if (cadeau.getRembourseLe() != null && !cadeau.estRemboursable()) {
            signalerVirementEnTrop(cadeau);
        }
    }

    @Transactional(readOnly = true)
    public boolean estRembourse(Long listeId, Long cadeauId, String login) {
        acces.proprietaire(listeId, login);
        return dansLaListe(listeId, cadeauId).getRembourseLe() != null;
    }

    /** Coche ou décoche le remboursement ; la date est celle du jour. */
    public void basculerRemboursement(Long listeId, Long cadeauId, String login) {
        acces.proprietaire(listeId, login);
        Cadeau cadeau = dansLaListe(listeId, cadeauId);
        if (cadeau.getRembourseLe() != null) {
            cadeau.setRembourseLe(null);
            return;
        }
        if (!cadeau.estRemboursable()) {
            throw new RequeteInvalideException(
                    "Ce cadeau n'a pas encore quitté votre compte courant : il ne peut pas être remboursé");
        }
        cadeau.setRembourseLe(dateDuJour.aujourdhui());
    }

    /** Marque d'un coup tous les cadeaux remboursables non remboursés ; une date nulle vaut aujourd'hui. */
    public ResultatRemboursement rembourserTout(Long listeId, String login, LocalDate date) {
        acces.proprietaire(listeId, login);
        LocalDate jour = date == null ? dateDuJour.aujourdhui() : date;
        if (jour.isAfter(dateDuJour.aujourdhui())) {
            throw new RequeteInvalideException("La date du virement ne peut pas être dans le futur");
        }
        BigDecimal somme = cadeaux.sommeARembourser(listeId);
        int nombre = cadeaux.marquerRembourses(listeId, jour);
        return new ResultatRemboursement(nombre, nombre == 0 ? BigDecimal.ZERO : somme);
    }

    public void changerStatut(Long listeId, Long cadeauId, String login, Statut statut) {
        acces.proprietaire(listeId, login);
        if (statut == null) {
            throw new RequeteInvalideException("Statut invalide");
        }
        Cadeau cadeau = dansLaListe(listeId, cadeauId);
        cadeau.setStatut(statut);
        if (statut == Statut.A_ACHETER && cadeau.getRembourseLe() != null) {
            signalerVirementEnTrop(cadeau);
        }
    }

    public void supprimer(Long listeId, Long cadeauId, String login) {
        acces.proprietaire(listeId, login);
        Cadeau cadeau = dansLaListe(listeId, cadeauId);
        if (cadeau.getRembourseLe() != null) {
            signalerVirementEnTrop(cadeau);
        }
        cadeaux.delete(cadeau);
    }

    // La date est effacée et l'alerte garde le montant déjà viré, que le propriétaire ferme lui-même.
    private void signalerVirementEnTrop(Cadeau cadeau) {
        alertes.signaler(cadeau.getListe(), cadeau.getNom(), cadeau.getPrix());
        cadeau.setRembourseLe(null);
    }

    private void verrouillerPrix(Cadeau cadeau, CadeauCommande commande) {
        BigDecimal nouveau = prixValide(commande.prix());
        BigDecimal ancien = cadeau.getPrix();
        boolean identique = ancien == null ? nouveau == null
                : nouveau != null && ancien.compareTo(nouveau) == 0;
        if (!identique) {
            throw new RequeteInvalideException(
                    "Le prix d'un cadeau remboursé ne peut pas être modifié : décochez d'abord le remboursement");
        }
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
