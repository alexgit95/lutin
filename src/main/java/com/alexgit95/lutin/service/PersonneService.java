package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.ListeAchatRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.vue.VuePersonneBudget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class PersonneService {

    public record ListeSource(Long id, String nom, long nombrePersonnes) {
    }

    public record PersonneImportable(Long id, String nom, BigDecimal budget, boolean dejaPresente) {
    }

    public record ApercuImport(Long sourceId, String sourceNom, List<PersonneImportable> personnes,
                               long nombreImportables, BigDecimal totalImportable) {
    }

    public record ResultatImport(int importees, int ignorees, BigDecimal budgetTotal) {
    }

    private static final BigDecimal BUDGET_MAX = new BigDecimal("10000000000");

    private final PersonneRepository personnes;
    private final CadeauRepository cadeaux;
    private final ListeAchatRepository listes;
    private final AccesListeService acces;

    public PersonneService(PersonneRepository personnes, CadeauRepository cadeaux, ListeAchatRepository listes,
                           AccesListeService acces) {
        this.personnes = personnes;
        this.cadeaux = cadeaux;
        this.listes = listes;
        this.acces = acces;
    }

    /** Personnes de la liste avec leur budget, ce qui est dépensé et ce qui reste à payer (propriétaire seulement). */
    @Transactional(readOnly = true)
    public List<VuePersonneBudget> lister(Long listeId, String login) {
        acces.proprietaire(listeId, login);
        Map<Long, Object[]> sommes = new HashMap<>();
        cadeaux.sommesParPersonne(listeId).forEach(ligne -> sommes.put((Long) ligne[0], ligne));
        return personnes.findByListeIdOrderByNom(listeId).stream().map(p -> {
            Object[] ligne = sommes.get(p.getId());
            return new VuePersonneBudget(p.getId(), p.getNom(), p.getBudget(),
                    ligne == null ? BigDecimal.ZERO : (BigDecimal) ligne[1],
                    ligne == null ? BigDecimal.ZERO : (BigDecimal) ligne[2]);
        }).toList();
    }

    public Personne creer(Long listeId, String login, String nom, BigDecimal budget) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        return personnes.save(new Personne(nomValide(nom), budgetValide(budget), liste));
    }

    @Transactional(readOnly = true)
    public Personne obtenir(Long listeId, Long personneId, String login) {
        acces.proprietaire(listeId, login);
        return dansLaListe(listeId, personneId);
    }

    public void modifier(Long listeId, Long personneId, String login, String nom, BigDecimal budget) {
        acces.proprietaire(listeId, login);
        Personne personne = dansLaListe(listeId, personneId);
        personne.setNom(nomValide(nom));
        personne.setBudget(budgetValide(budget));
    }

    /** Supprime la personne et tous les cadeaux qui lui sont destinés. */
    public void supprimer(Long listeId, Long personneId, String login) {
        acces.proprietaire(listeId, login);
        Personne personne = dansLaListe(listeId, personneId);
        cadeaux.deleteByPersonneId(personneId);
        personnes.delete(personne);
    }

    /** Autres listes dont l'utilisateur est propriétaire, avec leur nombre de personnes. */
    @Transactional(readOnly = true)
    public List<ListeSource> sourcesPossibles(Long listeId, String login) {
        acces.proprietaire(listeId, login);
        return listes.findByProprietaireLoginOrderByNom(login).stream()
                .filter(l -> !l.getId().equals(listeId))
                .map(l -> new ListeSource(l.getId(), l.getNom(), personnes.countByListeId(l.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApercuImport apercuImport(Long listeId, Long sourceId, String login) {
        ListeAchat source = controlerImport(listeId, sourceId, login);
        Set<String> presents = nomsDeLaListe(listeId);
        List<PersonneImportable> lignes = personnes.findByListeIdOrderByNom(sourceId).stream()
                .map(p -> new PersonneImportable(p.getId(), p.getNom(), p.getBudget(),
                        presents.contains(cle(p.getNom()))))
                .toList();
        List<PersonneImportable> importables = lignes.stream().filter(p -> !p.dejaPresente()).toList();
        BigDecimal total = importables.stream().map(PersonneImportable::budget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ApercuImport(sourceId, source.getNom(), lignes, importables.size(), total);
    }

    /**
     * Copie nom et budget des personnes cochées de la source. Tout est relu en base : les identifiants
     * qui n'appartiennent pas à la source sont ignorés, et les doublons sont recalculés à cet instant.
     */
    public ResultatImport importer(Long listeId, Long sourceId, String login, Collection<Long> ids) {
        controlerImport(listeId, sourceId, login);
        ListeAchat cible = acces.proprietaire(listeId, login);
        Set<Long> choisis = ids == null ? Set.of() : new HashSet<>(ids);
        Set<String> presents = nomsDeLaListe(listeId);
        int importees = 0;
        int ignorees = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Personne p : personnes.findByListeIdOrderByNom(sourceId)) {
            if (!choisis.contains(p.getId())) {
                continue;
            }
            if (presents.contains(cle(p.getNom()))) {
                ignorees++;
                continue;
            }
            personnes.save(new Personne(p.getNom(), p.getBudget(), cible));
            importees++;
            total = total.add(p.getBudget());
        }
        return new ResultatImport(importees, ignorees, total);
    }

    // Les deux listes doivent appartenir à l'utilisateur ; une liste simplement partagée est refusée (403).
    private ListeAchat controlerImport(Long listeId, Long sourceId, String login) {
        acces.proprietaire(listeId, login);
        ListeAchat source = acces.proprietaire(sourceId, login);
        if (listeId.equals(sourceId)) {
            throw new RequeteInvalideException("Choisissez une autre liste que la liste courante");
        }
        return source;
    }

    // Noms de la cible avant import : deux homônymes de la source ne s'excluent donc pas entre eux.
    private Set<String> nomsDeLaListe(Long listeId) {
        Set<String> noms = new HashSet<>();
        personnes.findByListeIdOrderByNom(listeId).forEach(p -> noms.add(cle(p.getNom())));
        return noms;
    }

    private static String cle(String nom) {
        return nom.trim().toLowerCase(Locale.ROOT);
    }

    private Personne dansLaListe(Long listeId, Long personneId) {
        Personne personne = personnes.findById(personneId)
                .orElseThrow(() -> new ElementIntrouvableException("Personne introuvable"));
        if (!personne.getListe().getId().equals(listeId)) {
            throw new ElementIntrouvableException("Personne introuvable");
        }
        return personne;
    }

    private String nomValide(String nom) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty() || propre.length() > 100) {
            throw new RequeteInvalideException("Le nom est obligatoire (100 caractères maximum)");
        }
        return propre;
    }

    private BigDecimal budgetValide(BigDecimal budget) {
        if (budget == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal arrondi = budget.setScale(2, RoundingMode.HALF_UP);
        if (arrondi.signum() < 0 || arrondi.compareTo(BUDGET_MAX) >= 0) {
            throw new RequeteInvalideException("Le budget doit être positif");
        }
        return arrondi;
    }
}
